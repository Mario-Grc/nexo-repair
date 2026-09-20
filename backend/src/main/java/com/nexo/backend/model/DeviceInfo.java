package com.nexo.backend.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class DeviceInfo {

    @Enumerated(EnumType.STRING)
    private DeviceType type;

    private String brand;
    private String model;
    private String identifier;

    public DeviceInfo() {
    }

    public DeviceInfo(DeviceType type, String brand, String model, String identifier) {
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.identifier = identifier;
    }

    public DeviceType getType() { return type; }
    public void setType(DeviceType type) { this.type = type; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
}
