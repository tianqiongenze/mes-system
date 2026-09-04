package com.mes.production.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
public class ProductionOrderRequest {
    @NotBlank private String productCode;
    @Positive private int quantity;
    private String priority = "NORMAL";
    private String workCenter;
    private String assignedOperator;
    public String getProductCode(){return productCode;} public void setProductCode(String p){this.productCode=p;}
    public int getQuantity(){return quantity;} public void setQuantity(int q){this.quantity=q;}
    public String getPriority(){return priority;} public void setPriority(String p){this.priority=p;}
    public String getWorkCenter(){return workCenter;} public void setWorkCenter(String w){this.workCenter=w;}
    public String getAssignedOperator(){return assignedOperator;} public void setAssignedOperator(String o){this.assignedOperator=o;}
}
