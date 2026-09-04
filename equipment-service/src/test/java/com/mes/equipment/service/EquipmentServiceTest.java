package com.mes.equipment.service;
import com.mes.cache.CacheMetrics;
import com.mes.equipment.dto.TelemetryRequest;
import com.mes.equipment.entity.Equipment;
import com.mes.equipment.repository.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EquipmentServiceTest {
    private EquipmentRepository repo;
    private EquipmentService service;
    @BeforeEach void setUp() { repo = mock(EquipmentRepository.class); service = new EquipmentService(repo, new CacheMetrics()); }

    @Test @DisplayName("register persists new equipment with IDLE status")
    void registerNew() {
        Equipment e = new Equipment("CNC-001", "CNC Mill", "Plant A");
        when(repo.findByCode("CNC-001")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Equipment saved = service.register(e);
        assertEquals("IDLE", saved.getStatus());
        verify(repo).save(any());
    }

    @Test @DisplayName("register rejects duplicate code")
    void registerDuplicate() {
        when(repo.findByCode("CNC-001")).thenReturn(Optional.of(new Equipment("CNC-001","x","y")));
        assertThrows(IllegalArgumentException.class, () -> service.register(new Equipment("CNC-001","x","y")));
    }

    @ParameterizedTest
    @CsvSource({"85.0,5.0,1200,MAINTENANCE", "95.0,3.0,1200,FAULT", "40.0,2.0,1200,RUNNING"})
    @DisplayName("predictStatus applies threshold rules")
    void predictStatusRules(double temp, double vib, int rpm, String expected) {
        TelemetryRequest t = new TelemetryRequest();
        t.setTemperature(temp); t.setVibration(vib); t.setRpm(rpm);
        assertEquals(expected, service.predictStatus(t));
    }

    @Test @DisplayName("ingestTelemetry flags FAULT on critical temperature")
    void ingestFlagsFault() {
        Equipment e = new Equipment("CNC-1","m","l"); e.setId("e1"); e.setStatus("RUNNING");
        when(repo.findById("e1")).thenReturn(Optional.of(e));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        TelemetryRequest t = new TelemetryRequest();
        t.setTemperature(96.0); t.setVibration(2.0); t.setRpm(1500);
        Equipment updated = service.ingestTelemetry("e1", t);
        assertEquals("FAULT", updated.getStatus());
        assertEquals(96.0, updated.getTemperature());
    }

    @Test @DisplayName("ingestTelemetry transitions IDLE->RUNNING when rpm>0")
    void ingestIdleToRunning() {
        Equipment e = new Equipment("CNC-1","m","l"); e.setId("e1"); e.setStatus("IDLE");
        when(repo.findById("e1")).thenReturn(Optional.of(e));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        TelemetryRequest t = new TelemetryRequest();
        t.setTemperature(40.0); t.setVibration(1.0); t.setRpm(1200);
        assertEquals("RUNNING", service.ingestTelemetry("e1", t).getStatus());
    }

    @Test @DisplayName("updateStatus validates and persists")
    void updateStatus() {
        Equipment e = new Equipment("CNC-1","m","l"); e.setId("e1");
        when(repo.findById("e1")).thenReturn(Optional.of(e));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("MAINTENANCE", service.updateStatus("e1","MAINTENANCE").getStatus());
        assertThrows(IllegalArgumentException.class, () -> service.updateStatus("e1","BROKEN"));
    }

    @Test @DisplayName("listByStatus validates status")
    void listByStatusValidates() {
        when(repo.findByStatus("RUNNING")).thenReturn(List.of());
        service.listByStatus("RUNNING");
        assertThrows(IllegalArgumentException.class, () -> service.listByStatus("FOO"));
    }
}
