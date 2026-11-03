package com.warehouse.domain;

public class Product {

    private int productId;
    private String name;
    private int threshold;
    private Supplier supplier;

    public Product(int productId, String name, int threshold, Supplier supplier) {
        setProductId(productId);
        this.name = name;
        this.threshold = threshold;
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
}
