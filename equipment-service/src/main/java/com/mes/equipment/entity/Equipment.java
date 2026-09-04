package com.mes.equipment.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "equipment")
public class Equipment {
    @Id
    private String id = UUID.randomUUID().toString();
    @Column(nullable=false, unique=true) private String code;
    @Column(nullable=false) private String name;
    @Column(nullable=false) private String status = "IDLE"; // IDLE, RUNNING, FAULT, MAINTENANCE, OFFLINE
    @Column(nullable=false) private String location;
    private String manufacturer;
    private String model;
    @Column(nullable=false) private double temperature;
    @Column(nullable=false) private double vibration;
    @Column(nullable=false) private int rpm;
    @Column(nullable=false) private double utilizationRate;
    private LocalDateTime lastMaintenance;
    @Column(nullable=false) private LocalDateTime installedAt = LocalDateTime.now();
    public Equipment() {}
    public Equipment(String code, String name, String location) {
        this.code = code; this.name = name; this.location = location;
    }
    public String getId(){return id;}
    public void setId(String id){this.id=id;}
    public String getCode(){return code;} public void setCode(String c){this.code=c;}
    public String getName(){return name;} public void setName(String n){this.name=n;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public String getLocation(){return location;} public void setLocation(String l){this.location=l;}
    public String getManufacturer(){return manufacturer;} public void setManufacturer(String m){this.manufacturer=m;}
    public String getModel(){return model;} public void setModel(String m){this.model=m;}
    public double getTemperature(){return temperature;} public void setTemperature(double t){this.temperature=t;}
    public double getVibration(){return vibration;} public void setVibration(double v){this.vibration=v;}
    public int getRpm(){return rpm;} public void setRpm(int r){this.rpm=r;}
    public double getUtilizationRate(){return utilizationRate;} public void setUtilizationRate(double u){this.utilizationRate=u;}
    public LocalDateTime getLastMaintenance(){return lastMaintenance;} public void setLastMaintenance(LocalDateTime t){this.lastMaintenance=t;}
    public LocalDateTime getInstalledAt(){return installedAt;} public void setInstalledAt(LocalDateTime t){this.installedAt=t;}
}
