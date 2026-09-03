package com.warehouse.storage;

import com.warehouse.domain.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SalesOrderDao implements Repository<SalesOrder,Integer>{

    private ProductDao productDao;
    private CustomerDao customerDao;
    private Connection connection;

    public SalesOrderDao(Connection connection,ProductDao productDao,CustomerDao customerDao){
        this.connection = connection;
        this.productDao = productDao;
        this.customerDao = customerDao;
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public Integer insert(SalesOrder item) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("INSERT INTO salesOrders (customerId,orderDate,orderStatus) VALUES  (?,?,?)",PreparedStatement.RETURN_GENERATED_KEYS))
        {
            ps.setInt(1,item.getCustomer().getCustomerId());
            ps.setString(2,item.getOrderDate().format(formatter));
            ps.setString(3,item.getOrderStatus().toString());

            ps.executeUpdate();

            try(ResultSet rs = ps.getGeneratedKeys())
            {
                if(rs.next()){
                    int newId = rs.getInt(1);
                    for(SalesOrderLineItem salesOrderLineItem : item.getSalesOrderLineItems())
                    {
                        try(PreparedStatement psLineItem = connection.prepareStatement("INSERT INTO salesOrderLineItems (salesOrderId,productId,quantity) VALUES (?,?,?)"))
                        {
                            psLineItem.setInt(1,newId);
                            psLineItem.setInt(2,salesOrderLineItem.getProduct().getProductId());
                            psLineItem.setInt(3,salesOrderLineItem.getQuantity());

                            psLineItem.executeUpdate();
                        }
                    }
                    return newId;
                }
            }
        }
        throw new IllegalArgumentException("No row was detected");
    }

    @Override
    public SalesOrder findById(Integer integer) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("SELECT  * FROM salesOrders WHERE salesOrderId = ?"))
        {
            ps.setInt(1,integer);
            try (ResultSet rs = ps.executeQuery())
            {
                if(rs.next())
                {
                    int customerId = rs.getInt("customerId");
                    Customer customer = customerDao.findById(customerId);

                    String orderDate = rs.getString("orderDate");
                    LocalDateTime date = LocalDateTime.parse(orderDate, formatter);

                    String orderStatus = rs.getString("orderStatus");

                    List<SalesOrderLineItem> salesOrderLineItems = new ArrayList<>();
                    try(PreparedStatement psLineItem = connection.prepareStatement("SELECT * FROM salesOrderLineItems WHERE salesOrderId = ?"))
                    {
                        psLineItem.setInt(1,integer);
                        try (ResultSet rsLineItem = psLineItem.executeQuery())
                        {
                            while (rsLineItem.next())
                            {
                                int productId = rsLineItem.getInt("productId");
                                int quantity = rsLineItem.getInt("quantity");

                                Product product = productDao.findById(productId);
                                salesOrderLineItems.add(new SalesOrderLineItem(product,quantity));
                            }
                        }
                    }

                    return new SalesOrder(integer,customer,date, OrderStatus.valueOf(orderStatus),salesOrderLineItems);
                }
            }
        }
        throw new IllegalArgumentException("No sales order found with ID: "+ integer + ".");
    }

    @Override
    public List<SalesOrder> findAll() throws SQLException {
        List<SalesOrder> salesOrders = new ArrayList<>();

        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM salesOrders");
            ResultSet rs = ps.executeQuery())
        {
            while (rs.next())
            {
                int salesOrderId = rs.getInt("salesOrderId");

                int customerId = rs.getInt("customerId");
                Customer customer = customerDao.findById(customerId);

                String orderDate = rs.getString("orderDate");
                LocalDateTime date = LocalDateTime.parse(orderDate, formatter);

                String orderStatus = rs.getString("orderStatus");

                List<SalesOrderLineItem> salesOrderLineItems = new ArrayList<>();
                try(PreparedStatement psLine = connection.prepareStatement("SELECT * FROM salesOrderLineItems WHERE salesOrderId = ?"))
                {
                    psLine.setInt(1,salesOrderId);
                    try(ResultSet rsLine = psLine.executeQuery())
                    {
                        while(rsLine.next())
                        {
                            int productId = rsLine.getInt("productId");
                            Product product = productDao.findById(productId);

                            int quantity = rsLine.getInt("quantity");

                            salesOrderLineItems.add(new SalesOrderLineItem(product,quantity));
                        }
                    }
                }
                salesOrders.add(new SalesOrder(salesOrderId,customer,date,OrderStatus.valueOf(orderStatus),salesOrderLineItems));
            }
        }
        return salesOrders;
    }

    @Override
    public void update(SalesOrder item) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("UPDATE salesOrders SET customerId = ?,orderDate = ?,orderStatus = ? WHERE salesOrderId = ?"))
        {
            ps.setInt(1,item.getCustomer().getCustomerId());
            ps.setString(2,item.getOrderDate().format(formatter));
            ps.setString(3,item.getOrderStatus().toString());
            ps.setInt(4,item.getSalesOrderId());

            ps.executeUpdate();
        }

        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM salesOrderLineItems WHERE salesOrderId = ?"))
        {
            ps.setInt(1,item.getSalesOrderId());

            ps.executeUpdate();
        }

        for(SalesOrderLineItem salesOrderLineItem : item.getSalesOrderLineItems())
        {
            try(PreparedStatement ps = connection.prepareStatement("INSERT INTO salesOrderLineItems (salesOrderId,productId,quantity) VALUES (?,?,?)"))
            {
                ps.setInt(1,item.getSalesOrderId());
                ps.setInt(2,salesOrderLineItem.getProduct().getProductId());
                ps.setInt(3,salesOrderLineItem.getQuantity());

                ps.executeUpdate();
            }
        }

    }

    @Override
    public void delete(Integer integer) throws SQLException {

        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM salesOrderLineItems WHERE salesOrderId = ?"))
        {
            ps.setInt(1,integer);

            ps.executeUpdate();
        }

        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM salesOrders WHERE salesOrderId = ?"))
        {
            ps.setInt(1,integer);

            ps.executeUpdate();
        }

    }
}
