package com.warehouse.domain;

public class StockItem {

    private Product product;
    private Bin bin;
    private int quantity;

    public StockItem(Product product, Bin bin, int quantity) {
        if(quantity < 0){
            throw new IllegalArgumentException("The quantity of the product can't be less than 0");
        }
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
