package com.mes.production.dto;
import java.time.LocalDateTime;
public record ProductionOrderResponse(String id, String productCode, int quantity, int completedQuantity,
        int defectCount, String status, String priority, String workCenter, String assignedOperator,
        LocalDateTime plannedStart, LocalDateTime plannedEnd, LocalDateTime actualStart, LocalDateTime actualEnd,
        LocalDateTime createdAt) {}
