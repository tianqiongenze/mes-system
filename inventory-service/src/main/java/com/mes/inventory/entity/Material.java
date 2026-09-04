package com.mes.inventory.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "materials")
public class Material {
    @Id
    private String id = UUID.randomUUID().toString();
    @Column(nullable=false, unique=true) private String sku;
    @Column(nullable=false) private String name;
    @Column(nullable=false) private String unit;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false) private int reorderPoint;
    @Column(nullable=false) private int safetyStock;
    @Column(nullable=false) private double unitCost;
    private String warehouse;
    @Column(nullable=false) private LocalDateTime updatedAt = LocalDateTime.now();
    public Material() {}
    public Material(String sku, String name, String unit, int quantity, int reorderPoint, int safetyStock, double unitCost) {
        this.sku = sku; this.name = name; this.unit = unit; this.quantity = quantity;
        this.reorderPoint = reorderPoint; this.safetyStock = safetyStock; this.unitCost = unitCost;
    }
    public String getId(){return id;}
    public String getSku(){return sku;} public void setSku(String s){this.sku=s;}
    public String getName(){return name;} public void setName(String n){this.name=n;}
    public String getUnit(){return unit;} public void setUnit(String u){this.unit=u;}
    public int getQuantity(){return quantity;} public void setQuantity(int q){this.quantity=q;}
    public int getReorderPoint(){return reorderPoint;} public void setReorderPoint(int r){this.reorderPoint=r;}
    public int getSafetyStock(){return safetyStock;} public void setSafetyStock(int s){this.safetyStock=s;}
    public double getUnitCost(){return unitCost;} public void setUnitCost(double c){this.unitCost=c;}
    public String getWarehouse(){return warehouse;} public void setWarehouse(String w){this.warehouse=w;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
}
