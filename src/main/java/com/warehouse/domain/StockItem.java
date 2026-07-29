package com.warehouse.domain;

public class StockItem {

    private Product product;
    private Bin bin;
    private int quantity;

    public StockItem(Product product, Bin bin, int quantity) {
        this.product = product;
        this.bin = bin;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public Bin getBin() {
        return bin;
    }

    public int getQuantity() {
        return quantity;
    }
}
