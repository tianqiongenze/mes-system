package com.mes.quality.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inspection_records")
public class InspectionRecord {
    @Id
    private String id = UUID.randomUUID().toString();
    @Column(nullable=false) private String productionOrderId;
    @Column(nullable=false) private String productCode;
    @Column(nullable=false) private int sampleSize;
    @Column(nullable=false) private int passed;
    @Column(nullable=false) private int failed;
    @Column(nullable=false) private String result = "PENDING"; // PASS, FAIL, PENDING
    private String inspector;
    private String defectType;
    private String notes;
    @Column(nullable=false) private LocalDateTime inspectedAt = LocalDateTime.now();
    public InspectionRecord() {}
    public InspectionRecord(String orderId, String productCode, int sampleSize, int passed, int failed) {
        this.productionOrderId = orderId; this.productCode = productCode;
        this.sampleSize = sampleSize; this.passed = passed; this.failed = failed;
        this.result = failed == 0 ? "PASS" : (passed == 0 ? "FAIL" : "FAIL");
    }
    public String getId(){return id;}
    public String getProductionOrderId(){return productionOrderId;} public void setProductionOrderId(String s){this.productionOrderId=s;}
    public String getProductCode(){return productCode;} public void setProductCode(String s){this.productCode=s;}
    public int getSampleSize(){return sampleSize;} public void setSampleSize(int s){this.sampleSize=s;}
    public int getPassed(){return passed;} public void setPassed(int p){this.passed=p;}
    public int getFailed(){return failed;} public void setFailed(int f){this.failed=f;}
    public String getResult(){return result;} public void setResult(String r){this.result=r;}
    public String getInspector(){return inspector;} public void setInspector(String i){this.inspector=i;}
    public String getDefectType(){return defectType;} public void setDefectType(String d){this.defectType=d;}
    public String getNotes(){return notes;} public void setNotes(String n){this.notes=n;}
    public LocalDateTime getInspectedAt(){return inspectedAt;} public void setInspectedAt(LocalDateTime t){this.inspectedAt=t;}
}
