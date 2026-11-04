package com.warehouse.domain;

public class Product {

    private int productId;
    private String name;
    private int threshold;
    private Supplier supplier;

    public Product(int productId, String name, int threshold, Supplier supplier) {
        setProductId(productId);
        setName(name);
        setThreshold(threshold);
        this.supplier = supplier;
    }

    public int getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public int getThreshold() {
        return threshold;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    private void setProductId(int productId){
        if(productId < 1){
            throw new IllegalArgumentException("Product ID must be at least 1");
        }
        this.productId = productId;
    }

    private void setThreshold(int threshold){
        if(threshold < 0){
            throw new IllegalArgumentException("Threshold must be at least 0");
        }
        this.threshold = threshold;
    }

    private void setName(String name){
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("Name can't be empty or null");
        }
        this.name = name;
    }
}
