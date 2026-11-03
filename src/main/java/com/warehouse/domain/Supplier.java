package com.warehouse.domain;

public class Supplier {

    private int supplierId;
    private String name;
    private String email;

    public Supplier(int supplierId, String name, String email) {
        setSupplierId(supplierId);
        this.name = name;
        this.email = email;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    private void setSupplierId(int supplierId){
        if(supplierId < 1){
            throw new IllegalArgumentException("Supplier ID must be at least 1");
        }
        this.supplierId = supplierId;
    }
}
