package com.warehouse.domain;

public class Bin {

    private int binId;
    private String locationCode;

    public Bin(int binId, String locationCode) {
        this.binId = binId;
        this.locationCode = locationCode;
    }

    public int getBinId() {
        return binId;
    }

    public String getLocationCode() {
        return locationCode;
    }
}
