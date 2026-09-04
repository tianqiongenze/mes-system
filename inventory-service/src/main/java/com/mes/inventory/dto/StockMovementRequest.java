package com.mes.inventory.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
public class StockMovementRequest {
    @NotBlank private String sku;
    @Positive private int quantity;
    private String type; // IN, OUT, ADJUST
    private String reference;
    public String getSku(){return sku;} public void setSku(String s){this.sku=s;}
    public int getQuantity(){return quantity;} public void setQuantity(int q){this.quantity=q;}
    public String getType(){return type;} public void setType(String t){this.type=t;}
    public String getReference(){return reference;} public void setReference(String r){this.reference=r;}
}
