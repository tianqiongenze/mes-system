package com.mes.quality.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
public class InspectionRequest {
    @NotBlank private String productionOrderId;
    @NotBlank private String productCode;
    @Positive private int sampleSize;
    @Positive private int passed;
    private int failed;
    private String inspector;
    private String defectType;
    public String getProductionOrderId(){return productionOrderId;} public void setProductionOrderId(String s){this.productionOrderId=s;}
    public String getProductCode(){return productCode;} public void setProductCode(String s){this.productCode=s;}
    public int getSampleSize(){return sampleSize;} public void setSampleSize(int s){this.sampleSize=s;}
    public int getPassed(){return passed;} public void setPassed(int p){this.passed=p;}
    public int getFailed(){return failed;} public void setFailed(int f){this.failed=f;}
    public String getInspector(){return inspector;} public void setInspector(String i){this.inspector=i;}
    public String getDefectType(){return defectType;} public void setDefectType(String d){this.defectType=d;}
}
