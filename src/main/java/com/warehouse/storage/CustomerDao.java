package com.warehouse.storage;

import com.warehouse.domain.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerDao {

    private Connection connection;

    public CustomerDao(Connection connection){
        this.connection = connection;
    }

    public void insert(Customer customer) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("INSERT INTO customers (customerId,name,email) VALUES (?,?,?)"))
        {
            ps.setInt(1,customer.getCustomerId());
            ps.setString(2,customer.getName());
            ps.setString(3,customer.getEmail());
            ps.executeUpdate();
        }
    }

    public Customer findById(int customerId) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM customers WHERE customerId = ?"))
        {
            ps.setInt(1,customerId);
            try(ResultSet rs = ps.executeQuery())
            {
                if(rs.next()){
                    String name = rs.getString("name");
                    String email = rs.getString("email");

                    Customer customer = new Customer(customerId,name,email);
                    return customer;
                }
            }
        }
        throw new IllegalArgumentException("No customer found with id: " + customerId + ".");
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM customers");
        ResultSet rs = ps.executeQuery())
        {
            while(rs.next()){
                int customerId = rs.getInt("customerId");
                String name = rs.getString("name");
                String email = rs.getString("email");

                Customer customer = new Customer(customerId,name,email);
                customers.add(customer);
            }
        }
        return customers;
    }

    public void update(Customer customer) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("UPDATE customers SET name = ? , email = ? WHERE customerId = ?"))
        {
            ps.setString(1,customer.getName());
            ps.setString(2,customer.getEmail());
            ps.setInt(3,customer.getCustomerId());

            ps.executeUpdate();
        }
    }

    public void delete(int customerId) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM customers WHERE customerId = ?"))
        {
            ps.setInt(1,customerId);
            ps.executeUpdate();
        }
    }
}
