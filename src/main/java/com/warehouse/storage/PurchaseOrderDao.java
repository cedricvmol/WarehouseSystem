package com.warehouse.storage;

import com.warehouse.domain.*;


import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrderDao implements Repository<PurchaseOrder,Integer>{

    private Connection connection;
    private SupplierDao supplierDao;
    private ProductDao productDao;

    public PurchaseOrderDao(Connection connection,SupplierDao supplierDao,ProductDao productDao){
        this.connection = connection;
        this.supplierDao = supplierDao;
        this.productDao = productDao;
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public Integer insert(PurchaseOrder item) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("INSERT INTO purchaseOrders (" +
                "supplierId,orderDate,orderStatus) VALUES  (?,?,?)",PreparedStatement.RETURN_GENERATED_KEYS)
            )
        {
            ps.setInt(1,item.getSupplier().getSupplierId());
            ps.setString(2,item.getOrderDate().format(formatter));
            ps.setString(3,item.getOrderStatus().toString());

            ps.executeUpdate();

            try(ResultSet keys = ps.getGeneratedKeys())
            {
                if(keys.next()){
                    int newId = keys.getInt(1);
                    for(PurchaseOrderLineItem lineItem : item.getPurchaseOrderLineItems()){
                        try(PreparedStatement psItems = connection.prepareStatement("INSERT INTO purchaseOrderLineItems(purchaseOrderId,productId,quantity) VALUES (?,?,?) "))
                        {
                            psItems.setInt(1,newId);
                            psItems.setInt(2,lineItem.getProduct().getProductId());
                            psItems.setInt(3,lineItem.getQuantity());

                            psItems.executeUpdate();
                        }
                    }
                    return newId;
                }
            }

        }
        throw new IllegalArgumentException("No row was detected.");
    }

    @Override
    public PurchaseOrder findById(Integer integer) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM purchaseOrders WHERE purchaseOrderId = ?"))
        {
            ps.setInt(1,integer);
            try(ResultSet rs = ps.executeQuery())
            {
                if(rs.next()){
                    int supplierId = rs.getInt("supplierId");
                    Supplier supplier = supplierDao.findById(supplierId);

                    String orderDate = rs.getString("orderDate");
                    LocalDateTime date = LocalDateTime.parse(orderDate, formatter);

                    String orderStatus = rs.getString("orderStatus");

                    List<PurchaseOrderLineItem> purchaseOrderLineItems = new ArrayList<>();
                    try(PreparedStatement psLineItem = connection.prepareStatement("SELECT * FROM purchaseOrderLineItems WHERE purchaseOrderId = ?"))
                    {
                        psLineItem.setInt(1,integer);
                        try(ResultSet rsLineItem = psLineItem.executeQuery())
                        {
                            while (rsLineItem.next()){
                                int productId = rsLineItem.getInt("productId");
                                int quantity = rsLineItem.getInt("quantity");

                                Product product = productDao.findById(productId);
                                purchaseOrderLineItems.add(new PurchaseOrderLineItem(product,quantity));
                            }
                        }
                    }
                    return new PurchaseOrder(integer,supplier,date, OrderStatus.valueOf(orderStatus),purchaseOrderLineItems);
                }
            }
        }
        throw new IllegalArgumentException("No purchase order found with ID: " + integer + ".");
    }

    @Override
    public List<PurchaseOrder> findAll() throws SQLException {

        List<PurchaseOrder> purchaseOrders = new ArrayList<>();

        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM purchaseOrders");
            ResultSet rs = ps.executeQuery())
        {
            while (rs.next()){

                int purchaseOrderId = rs.getInt("purchaseOrderId");
                int supplierId = rs.getInt("supplierId");
                Supplier supplier = supplierDao.findById(supplierId);

                String orderDate = rs.getString("orderDate");
                LocalDateTime date = LocalDateTime.parse(orderDate, formatter);

                String orderStatus = rs.getString("orderStatus");

                List<PurchaseOrderLineItem> purchaseOrderLineItems = new ArrayList<>();
                try(PreparedStatement psLine = connection.prepareStatement("SELECT * FROM purchaseOrderLineItems WHERE purchaseOrderId = ?"))
                {
                    psLine.setInt(1,purchaseOrderId);
                    try(ResultSet rsLine = psLine.executeQuery())
                    {
                        while(rsLine.next()){
                            int productId = rsLine.getInt("productId");
                            Product product = productDao.findById(productId);

                            int quantity = rsLine.getInt("quantity");

                            PurchaseOrderLineItem purchaseOrderLineItem = new PurchaseOrderLineItem(product,quantity);
                            purchaseOrderLineItems.add(purchaseOrderLineItem);
                        }
                    }
                }
                purchaseOrders.add(new PurchaseOrder(purchaseOrderId,supplier,date,OrderStatus.valueOf(orderStatus),purchaseOrderLineItems));
            }
        }
        return purchaseOrders;
    }

    @Override
    public void update(PurchaseOrder item) throws SQLException {

        try(PreparedStatement ps = connection.prepareStatement("UPDATE purchaseOrders SET supplierId = ? , orderDate = ? , orderStatus = ? WHERE purchaseOrderId = ?"))
        {
            ps.setInt(1,item.getSupplier().getSupplierId());
            ps.setString(2,item.getOrderDate().format(formatter));
            ps.setString(3,item.getOrderStatus().toString());
            ps.setInt(4,item.getPurchaseOrderId());

            ps.executeUpdate();
        }

        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM purchaseOrderLineItems WHERE purchaseOrderId = ?"))
        {
            ps.setInt(1,item.getPurchaseOrderId());

            ps.executeUpdate();
        }
        for(PurchaseOrderLineItem items : item.getPurchaseOrderLineItems()){
            try(PreparedStatement psItems = connection.prepareStatement("INSERT INTO purchaseOrderLineItems(purchaseOrderId,productId,quantity) VALUES (?,?,?) "))
            {
                psItems.setInt(1,item.getPurchaseOrderId());
                psItems.setInt(2,items.getProduct().getProductId());
                psItems.setInt(3,items.getQuantity());
                psItems.executeUpdate();
            }
        }
    }

    @Override
    public void delete(Integer integer) throws SQLException {

        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM purchaseOrderLineItems WHERE purchaseOrderId = ?"))
        {
            ps.setInt(1,integer);

            ps.executeUpdate();
        }

        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM purchaseOrders WHERE purchaseOrderId = ?"))
        {
            ps.setInt(1,integer);

            ps.executeUpdate();
        }

    }
}
