package com.mes.production.service;
import com.mes.cache.CacheMetrics;

import com.mes.production.dto.ProductionOrderRequest;
import com.mes.production.dto.ProductionOrderResponse;
import com.mes.production.entity.ProductionOrder;
import com.mes.production.repository.ProductionOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductionServiceTest {

    private ProductionOrderRepository repo;
    private ProductionService service;

    @BeforeEach
    void setUp() {
        repo = mock(ProductionOrderRepository.class);
        service = new ProductionService(repo, new CacheMetrics());
    }

    private ProductionOrderRequest req(String product, int qty, String priority) {
        ProductionOrderRequest r = new ProductionOrderRequest();
        r.setProductCode(product);
        r.setQuantity(qty);
        r.setPriority(priority);
        r.setWorkCenter("WC-1");
        return r;
    }

    @Test
    @DisplayName("createOrder persists a CREATED order and returns its data")
    void createOrderPersistsCreatedOrder() {
        when(repo.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductionOrderResponse r = service.createOrder(req("P-A100", 100, "HIGH"));
        assertNotNull(r.id());
        assertEquals("P-A100", r.productCode());
        assertEquals(100, r.quantity());
        assertEquals("CREATED", r.status());
        assertEquals("HIGH", r.priority());
        assertEquals(0, r.completedQuantity());
        verify(repo).save(any(ProductionOrder.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOW", "NORMAL", "HIGH", "URGENT"})
    @DisplayName("createOrder accepts all valid priorities")
    void acceptsValidPriorities(String priority) {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(priority, service.createOrder(req("P", 1, priority)).priority());
    }

    @Test
    @DisplayName("createOrder rejects invalid priority")
    void rejectsInvalidPriority() {
        assertThrows(IllegalArgumentException.class, () -> service.createOrder(req("P", 10, "CRITICAL")));
    }

    @Test
    @DisplayName("createOrder rejects non-positive quantity")
    void rejectsNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> service.createOrder(req("P", 0, "NORMAL")));
        assertThrows(IllegalArgumentException.class, () -> service.createOrder(req("P", -5, "NORMAL")));
    }

    @Test
    @DisplayName("getOrder throws when not found")
    void getOrderThrowsWhenMissing() {
        when(repo.findById("X")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getOrder("X"));
    }

    @Nested
    @DisplayName("startProduction lifecycle")
    class StartProduction {
        @Test
        void startsCreatedOrder() {
            ProductionOrder o = new ProductionOrder("P", 50, "NORMAL");
            o.setId("ord-1");
            when(repo.findById("ord-1")).thenReturn(Optional.of(o));
            when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
            ProductionOrderResponse r = service.startProduction("ord-1");
            assertEquals("IN_PROGRESS", r.status());
            assertNotNull(r.actualStart());
        }
        @Test
        void cannotStartAlreadyInProgress() {
            ProductionOrder o = new ProductionOrder("P", 50, "NORMAL");
            o.setStatus("IN_PROGRESS");
            when(repo.findById("ord-2")).thenReturn(Optional.of(o));
            assertThrows(IllegalStateException.class, () -> service.startProduction("ord-2"));
        }
    }

    @Nested
    @DisplayName("reportProgress lifecycle")
    class ReportProgress {
        @Test
        void accumulatesProgressAndAutoCompletes() {
            ProductionOrder o = new ProductionOrder("P", 100, "NORMAL");
            o.setId("ord-3");
            o.setStatus("IN_PROGRESS");
            when(repo.findById("ord-3")).thenReturn(Optional.of(o));
            when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
            // first batch: 60 good, 2 bad
            ProductionOrderResponse r1 = service.reportProgress("ord-3", 60, 2);
            assertEquals(60, r1.completedQuantity());
            assertEquals(2, r1.defectCount());
            assertEquals("IN_PROGRESS", r1.status());
            // second batch: 40 good -> reaches quantity -> COMPLETED
            ProductionOrderResponse r2 = service.reportProgress("ord-3", 40, 0);
            assertEquals(100, r2.completedQuantity());
            assertEquals("COMPLETED", r2.status());
            assertNotNull(r2.actualEnd());
        }
        @Test
        void rejectsProgressOnCreatedOrder() {
            ProductionOrder o = new ProductionOrder("P", 10, "NORMAL");
            when(repo.findById("x")).thenReturn(Optional.of(o));
            assertThrows(IllegalStateException.class, () -> service.reportProgress("x", 5, 0));
        }
        @Test
        void rejectsNegativeProgress() {
            ProductionOrder o = new ProductionOrder("P", 10, "NORMAL");
            o.setStatus("IN_PROGRESS");
            when(repo.findById("x")).thenReturn(Optional.of(o));
            assertThrows(IllegalArgumentException.class, () -> service.reportProgress("x", -1, 0));
            assertThrows(IllegalArgumentException.class, () -> service.reportProgress("x", 0, -1));
        }
    }

    @Nested
    @DisplayName("cancelOrder lifecycle")
    class CancelOrder {
        @Test
        void cancelsInProgressOrder() {
            ProductionOrder o = new ProductionOrder("P", 10, "NORMAL");
            o.setStatus("IN_PROGRESS");
            when(repo.findById("c")).thenReturn(Optional.of(o));
            when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
            assertEquals("CANCELLED", service.cancelOrder("c").status());
        }
        @Test
        void cannotCancelCompletedOrder() {
            ProductionOrder o = new ProductionOrder("P", 10, "NORMAL");
            o.setStatus("COMPLETED");
            when(repo.findById("c")).thenReturn(Optional.of(o));
            assertThrows(IllegalStateException.class, () -> service.cancelOrder("c"));
        }
    }

    @Test
    @DisplayName("calculateYieldRate returns 0 when nothing completed")
    void yieldRateZeroWhenEmpty() {
        when(repo.totalQuantityByStatus("COMPLETED")).thenReturn(0);
        when(repo.totalDefects()).thenReturn(0);
        assertEquals(0.0, service.calculateYieldRate());
    }

    @Test
    @DisplayName("calculateYieldRate computes (completed-defects)/completed*100")
    void yieldRateComputation() {
        when(repo.totalQuantityByStatus("COMPLETED")).thenReturn(100);
        when(repo.totalDefects()).thenReturn(5);
        assertEquals(95.0, service.calculateYieldRate(), 0.0001);
    }

    @Test
    @DisplayName("listByStatus validates status and delegates to repo")
    void listByStatusDelegates() {
        ProductionOrder o = new ProductionOrder("P", 1, "NORMAL");
        o.setStatus("CREATED");
        when(repo.findByStatus("CREATED")).thenReturn(List.of(o));
        List<ProductionOrderResponse> res = service.listByStatus("CREATED");
        assertEquals(1, res.size());
        assertEquals("CREATED", res.get(0).status());
    }

    @Test
    @DisplayName("listByStatus rejects invalid status")
    void listByStatusRejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.listByStatus("FOO"));
    }
}
