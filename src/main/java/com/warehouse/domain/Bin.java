package com.warehouse.domain;

public class Bin {

    private int binId;
    private String locationCode;

    public Bin(int binId, String locationCode) {
        setBinId(binId);
        setLocationCode(locationCode);
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

    private void setLocationCode(String locationCode){
        if(locationCode == null || locationCode.isBlank()){
            throw new IllegalArgumentException("Location code can't be empty or null");
        }
        this.locationCode = locationCode;
    }
}
