package com.mes.inventory.service;
import com.mes.cache.CacheMetrics;
import com.mes.inventory.dto.StockMovementRequest;
import com.mes.inventory.entity.Material;
import com.mes.inventory.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceTest {
    private MaterialRepository repo;
    private InventoryService service;
    @BeforeEach void setUp() { repo = mock(MaterialRepository.class); service = new InventoryService(repo, new CacheMetrics()); }

    private Material mat(int qty, int reorder, int safety) {
        Material m = new Material("SKU-1", "Bolt", "pcs", qty, reorder, safety, 0.5);
        return m;
    }
    private StockMovementRequest mv(String sku, int qty, String type) {
        StockMovementRequest r = new StockMovementRequest();
        r.setSku(sku); r.setQuantity(qty); r.setType(type);
        return r;
    }

    @Test @DisplayName("createMaterial rejects duplicate SKU")
    void createDuplicate() {
        when(repo.findBySku("SKU-1")).thenReturn(Optional.of(mat(10,5,2)));
        assertThrows(IllegalArgumentException.class, () -> service.createMaterial(mat(10,5,2)));
    }

    @Test @DisplayName("moveStock IN increases quantity")
    void moveIn() {
        Material m = mat(10,5,2);
        when(repo.findBySku("SKU-1")).thenReturn(Optional.of(m));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Material res = service.moveStock(mv("SKU-1", 5, "IN"));
        assertEquals(15, res.getQuantity());
    }

    @Test @DisplayName("moveStock OUT decreases quantity")
    void moveOut() {
        Material m = mat(10,5,2);
        when(repo.findBySku("SKU-1")).thenReturn(Optional.of(m));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(7, service.moveStock(mv("SKU-1", 3, "OUT")).getQuantity());
    }

    @Test @DisplayName("moveStock OUT rejects insufficient stock")
    void moveOutInsufficient() {
        Material m = mat(3,5,2);
        when(repo.findBySku("SKU-1")).thenReturn(Optional.of(m));
        assertThrows(IllegalStateException.class, () -> service.moveStock(mv("SKU-1", 10, "OUT")));
    }

    @Test @DisplayName("moveStock ADJUST applies signed delta")
    void moveAdjust() {
        Material m = mat(10,5,2);
        when(repo.findBySku("SKU-1")).thenReturn(Optional.of(m));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        StockMovementRequest r = mv("SKU-1", -2, "ADJUST");
        assertEquals(8, service.moveStock(r).getQuantity());
    }

    @Test @DisplayName("moveStock rejects invalid type")
    void moveInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> service.moveStock(mv("SKU-1", 1, "TRANSFER")));
    }

    @Test @DisplayName("moveStock rejects unknown SKU")
    void moveUnknownSku() {
        when(repo.findBySku("X")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.moveStock(mv("X", 1, "IN")));
    }
}
