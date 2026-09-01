package com.warehouse.domain;

import java.time.LocalDateTime;
import java.util.List;

public class PurchaseOrder {

    private int purchaseOrderId;
    private Supplier supplier;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private List<PurchaseOrderLineItem> purchaseOrderLineItems;

    public PurchaseOrder(int purchaseOrderId,Supplier supplier,LocalDateTime orderDate,OrderStatus orderStatus,List<PurchaseOrderLineItem> purchaseOrderLineItems){
        this.purchaseOrderId = purchaseOrderId;
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.purchaseOrderLineItems = purchaseOrderLineItems;
    }


    public int getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public OrderStatus getOrderStatus(){
        return orderStatus;
    }

    public List<PurchaseOrderLineItem> getPurchaseOrderLineItems() {
        return purchaseOrderLineItems;
    }

}
