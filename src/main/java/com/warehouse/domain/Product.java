package com.warehouse.domain;

public class Product {

    private int productId;
    private String name;
    private int threshold;
    private Supplier supplier;

    public Product(int productId, String name, int threshold, Supplier supplier) {
        this.productId = productId;
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
}
