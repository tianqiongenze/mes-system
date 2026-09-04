package com.mes.quality.service;
import com.mes.cache.CacheMetrics;
import com.mes.quality.dto.InspectionRequest;
import com.mes.quality.entity.InspectionRecord;
import com.mes.quality.repository.InspectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QualityServiceTest {
    private InspectionRepository repo;
    private QualityService service;
    @BeforeEach void setUp() { repo = mock(InspectionRepository.class); service = new QualityService(repo, new CacheMetrics()); }

    private InspectionRequest req(int sample, int passed, int failed) {
        InspectionRequest r = new InspectionRequest();
        r.setProductionOrderId("ord-1"); r.setProductCode("P-A100");
        r.setSampleSize(sample); r.setPassed(passed); r.setFailed(failed);
        r.setInspector("insp-1");
        return r;
    }

    @Test @DisplayName("recordInspection saves a PASS when no failures")
    void recordsPass() {
        when(repo.save(any(InspectionRecord.class))).thenAnswer(i -> i.getArgument(0));
        InspectionRecord rec = service.recordInspection(req(100, 100, 0));
        assertEquals("PASS", rec.getResult());
        assertEquals(100, rec.getPassed());
        verify(repo).save(any());
    }

    @Test @DisplayName("recordInspection saves a FAIL when there are failures")
    void recordsFail() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        InspectionRecord rec = service.recordInspection(req(100, 92, 8));
        assertEquals("FAIL", rec.getResult());
        assertEquals(8, rec.getFailed());
    }

    @ParameterizedTest
    @CsvSource({"0,-1", "-1,0"})
    @DisplayName("recordInspection rejects negative counts")
    void rejectsNegative(int passed, int failed) {
        InspectionRequest r = req(10, Math.max(passed,0), Math.max(failed,0));
        r.setPassed(passed); r.setFailed(failed);
        assertThrows(IllegalArgumentException.class, () -> service.recordInspection(r));
    }

    @Test @DisplayName("recordInspection rejects passed+failed != sampleSize")
    void rejectsMismatchedCounts() {
        InspectionRequest r = req(100, 50, 40); // 50+40 != 100
        assertThrows(IllegalArgumentException.class, () -> service.recordInspection(r));
    }

    @Test @DisplayName("getInspection throws when missing")
    void getThrows() {
        when(repo.findById("X")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getInspection("X"));
    }

    @Test @DisplayName("firstPassYield and defectRate compute correctly")
    void metricsCompute() {
        when(repo.totalPassed()).thenReturn(90);
        when(repo.totalFailed()).thenReturn(10);
        assertEquals(90.0, service.firstPassYield(), 0.001);
        assertEquals(10.0, service.defectRate(), 0.001);
    }

    @Test @DisplayName("metrics are 0 when no data")
    void metricsZeroWhenEmpty() {
        when(repo.totalPassed()).thenReturn(0);
        when(repo.totalFailed()).thenReturn(0);
        assertEquals(0.0, service.firstPassYield());
        assertEquals(0.0, service.defectRate());
    }
}
