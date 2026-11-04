package com.warehouse.domain;

public class Supplier {

    private int supplierId;
    private String name;
    private String email;

    public Supplier(int supplierId, String name, String email) {
        setSupplierId(supplierId);
        setName(name);
        setEmail(email);
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

    private void setName(String name){
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("Name can't be empty or null");
        }
        this.name = name;
    }

    private void setEmail(String email){
        if(email == null || email.isBlank()){
            throw new IllegalArgumentException("Email can't be empty or null");
        }
        this.email = email;
    }
}
