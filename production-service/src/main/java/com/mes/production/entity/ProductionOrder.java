package com.mes.production.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "production_orders")
public class ProductionOrder {
    @Id
    private String id = UUID.randomUUID().toString();
    @Column(nullable=false)
    private String productCode;
    @Column(nullable=false)
    private int quantity;
    @Column(nullable=false)
    private String status = "CREATED"; // CREATED, IN_PROGRESS, COMPLETED, CANCELLED
    @Column(nullable=false)
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT
    private String workCenter;
    private String assignedOperator;
    private int completedQuantity;
    private int defectCount;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    @Column(nullable=false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    public ProductionOrder() {}
    public ProductionOrder(String productCode, int quantity, String priority) {
        this.productCode = productCode; this.quantity = quantity; this.priority = priority;
    }
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public String getProductCode(){return productCode;} public void setProductCode(String p){this.productCode=p;}
    public int getQuantity(){return quantity;} public void setQuantity(int q){this.quantity=q;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public String getPriority(){return priority;} public void setPriority(String p){this.priority=p;}
    public String getWorkCenter(){return workCenter;} public void setWorkCenter(String w){this.workCenter=w;}
    public String getAssignedOperator(){return assignedOperator;} public void setAssignedOperator(String o){this.assignedOperator=o;}
    public int getCompletedQuantity(){return completedQuantity;} public void setCompletedQuantity(int c){this.completedQuantity=c;}
    public int getDefectCount(){return defectCount;} public void setDefectCount(int d){this.defectCount=d;}
    public LocalDateTime getPlannedStart(){return plannedStart;} public void setPlannedStart(LocalDateTime t){this.plannedStart=t;}
    public LocalDateTime getPlannedEnd(){return plannedEnd;} public void setPlannedEnd(LocalDateTime t){this.plannedEnd=t;}
    public LocalDateTime getActualStart(){return actualStart;} public void setActualStart(LocalDateTime t){this.actualStart=t;}
    public LocalDateTime getActualEnd(){return actualEnd;} public void setActualEnd(LocalDateTime t){this.actualEnd=t;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
}
