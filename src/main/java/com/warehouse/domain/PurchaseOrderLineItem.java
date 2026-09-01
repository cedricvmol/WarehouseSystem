package com.warehouse.domain;

public class PurchaseOrderLineItem {

    private Product product;
    private int quantity;

    public PurchaseOrderLineItem(Product product , int quantity){
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
