package com.mes.equipment.dto;
public class TelemetryRequest {
    private double temperature;
    private double vibration;
    private int rpm;
    public double getTemperature(){return temperature;} public void setTemperature(double t){this.temperature=t;}
    public double getVibration(){return vibration;} public void setVibration(double v){this.vibration=v;}
    public int getRpm(){return rpm;} public void setRpm(int r){this.rpm=r;}
}
