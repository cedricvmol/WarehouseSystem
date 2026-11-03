package com.warehouse.domain;

public class Bin {

    private int binId;
    private String locationCode;

    public Bin(int binId, String locationCode) {
        setBinId(binId);
        this.locationCode = locationCode;
    }

    public int getBinId() {
        return binId;
    }

    public String getLocationCode() {
        return locationCode;
    }

    private void setBinId(int binId){
        if(binId < 1){
            throw new IllegalArgumentException("Bin ID must be at least 1");
        }
        this.binId = binId;
    }
}
