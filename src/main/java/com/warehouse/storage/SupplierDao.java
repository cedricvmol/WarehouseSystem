package com.warehouse.storage;

import com.warehouse.domain.Product;
import com.warehouse.domain.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SupplierDao {

    private Connection connection;

    public SupplierDao(Connection connection){
        this.connection = connection;
    }

    public void insert(Supplier supplier) throws SQLException{
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO suppliers (supplierId,name,email) VALUES (?,?,?)"))
        {
            ps.setInt(1,supplier.getSupplierId());
            ps.setString(2,supplier.getName());
            ps.setString(3,supplier.getEmail());

            ps.executeUpdate();
        }
    }

    public Supplier findById(int supplierId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM suppliers WHERE supplierId = ?"))
        {
            ps.setInt(1,supplierId);

            try(ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    String name = rs.getString("name");
                    String email = rs.getString("email");

                    Supplier supplier = new Supplier(supplierId,name,email);
                    return supplier;
                }
            }
        }
        throw new IllegalArgumentException("No supplier found with id: " + supplierId + ".");
    }

    public List<Supplier> findAll() throws SQLException{
        List<Supplier> suppliers = new ArrayList<>();
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM suppliers");
            ResultSet rs = ps.executeQuery())
        {
            while (rs.next()){
                int supplierId = rs.getInt("supplierId");
                String name = rs.getString("name");
                String email = rs.getString("email");

                Supplier supplier = new Supplier(supplierId,name,email);
                suppliers.add(supplier);
            }
        }
        return suppliers;
    }

    public void update(Supplier supplier) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("UPDATE suppliers SET name = ? , email = ? WHERE supplierId = ?"))
        {
            ps.setString(1,supplier.getName());
            ps.setString(2,supplier.getEmail());
            ps.setInt(3,supplier.getSupplierId());

            ps.executeUpdate();
        }
    }

    public void delete(int supplierId) throws SQLException{
        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM suppliers WHERE supplierId = ?")){
            ps.setInt(1,supplierId);

            ps.executeUpdate();
        }
    }


}
