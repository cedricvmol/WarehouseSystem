package com.warehouse.storage;

import com.warehouse.domain.Product;
import com.warehouse.domain.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class ProductDao {

    private Connection connection;
    private SupplierDao supplierDao;

    public ProductDao(Connection connection, SupplierDao supplierDao){
        this.connection = connection;
        this.supplierDao = supplierDao;
    }

    public void insert(Product product) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO products (" +
                         "productId,name,threshold,supplierId) VALUES (?,?,?,?)"))
        {
            preparedStatement.setInt(1,product.getProductId());
            preparedStatement.setString(2,product.getName());
            preparedStatement.setInt(3,product.getThreshold());
            preparedStatement.setInt(4,product.getSupplier().getSupplierId());

            preparedStatement.executeUpdate();
        }
    }

    public Product findById(int productId) throws SQLException {
        try(PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM products WHERE productId = ?"))
        {
            preparedStatement.setInt(1,productId);
            try (ResultSet rs = preparedStatement.executeQuery()){

                if(rs.next()){
                    String name = rs.getString("name");
                    int threshold = rs.getInt("threshold");
                    int supplierId = rs.getInt("supplierId");

                    Supplier supplier = supplierDao.findById(supplierId);
                    Product product = new Product(productId,name,threshold,supplier);
                    return product;
                }
            }
        }
        throw new IllegalArgumentException("No product found with id: " + productId + ".");
    }

    public List<Product> findAll() throws SQLException{
        List<Product> products = new ArrayList<>();
        try(PreparedStatement ps = connection.prepareStatement("SELECT * FROM products");
            ResultSet rs = ps.executeQuery())
        {
            while (rs.next()){
                int productId = rs.getInt("productId");
                String name = rs.getString("name");
                int threshold = rs.getInt("threshold");
                int supplierId = rs.getInt("supplierId");

                Supplier supplier = supplierDao.findById(supplierId);

                Product product = new Product(productId,name,threshold,supplier);
                products.add(product);

            }
        }
        return products;
    }

    public void update(Product product) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("UPDATE products SET name = ?, threshold = ? , supplierId = ? WHERE productId = ? "))
        {
            ps.setString(1,product.getName());
            ps.setInt(2,product.getThreshold());
            ps.setInt(3,product.getSupplier().getSupplierId());
            ps.setInt(4,product.getProductId());

            ps.executeUpdate();
        }
    }

    public void delete(int productId) throws SQLException {
        try(PreparedStatement ps = connection.prepareStatement("DELETE FROM products WHERE productId = ?"))
        {
            ps.setInt(1,productId);

            ps.executeUpdate();
        }
    }


}
