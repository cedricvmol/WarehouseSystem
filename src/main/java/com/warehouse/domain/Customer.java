package com.warehouse.domain;

public class Customer {

    private int customerId;
    private String name;
    private String email;

    public Customer(int customerId, String name, String email) {
        setCustomerId(customerId);
        setName(name);
        setEmail(email);
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    private void setCustomerId(int customerId){
        if(customerId < 1){
            throw new IllegalArgumentException("Customer ID must be at least 1");
        }
        this.customerId = customerId;
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
        if(!email.matches("[^@]+@[^@]+")){
            throw new IllegalArgumentException("Email is not valid.");
        }
        this.email = email;
    }
}
