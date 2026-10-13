package com.warehouse.domain;

import java.time.LocalDateTime;
import java.util.List;

public class SalesOrder {

    private int salesOrderId;
    private Customer customer;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private List<SalesOrderLineItem> salesOrderLineItems;

    public SalesOrder(int salesOrderId, Customer customer, LocalDateTime orderDate, OrderStatus orderStatus, List<SalesOrderLineItem> salesOrderLineItems) {
        if(salesOrderLineItems == null || salesOrderLineItems.isEmpty()) throw new IllegalArgumentException("There are no line items in your sales order.");
        this.salesOrderId = salesOrderId;
        this.customer = customer;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.salesOrderLineItems = salesOrderLineItems;
    }

     public SalesOrder(Customer customer,List<SalesOrderLineItem> salesOrderLineItems){
        this(0,customer,LocalDateTime.now(),OrderStatus.OPEN,salesOrderLineItems);
     }


    public int getSalesOrderId() {
        return salesOrderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public List<SalesOrderLineItem> getSalesOrderLineItems() {
        return salesOrderLineItems;
    }

    public void cancel(){
        if(this.orderStatus != OrderStatus.OPEN){
            throw new IllegalStateException("Order is already canceled or already completed.");
        }
        this.orderStatus = OrderStatus.CANCELLED;
    }
}
