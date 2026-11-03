package com.warehouse.domain;

public class SalesOrderLineItem {

    private Product product;
    private int quantity;

    public SalesOrderLineItem(Product product, int quantity){
        if(quantity <= 0){
            throw new IllegalArgumentException("The quantity of the product must be higher than 0");
        }
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }
}
