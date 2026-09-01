package com.warehouse.service;

import com.warehouse.domain.*;
import com.warehouse.storage.*;

import java.sql.SQLException;
import java.util.List;

public class WarehouseService {

    private ProductDao productDao;
    private BinDao binDao;
    private CustomerDao customerDao;
    private SupplierDao supplierDao;
    private StockItemDao stockItemDao;

    public WarehouseService(ProductDao productDao,BinDao binDao,CustomerDao customerDao,SupplierDao supplierDao,StockItemDao stockItemDao){
        this.productDao = productDao;
        this.binDao = binDao;
        this.customerDao = customerDao;
        this.supplierDao = supplierDao;
        this.stockItemDao = stockItemDao;
    }

    public void addProduct(int productId,String name, int threshold,int supplierId) throws SQLException {

        Supplier supplier = findSupplier(supplierId);

        if(supplier == null) {
            throw new IllegalArgumentException("No supplier found with id: " + supplierId + ".");
        }
        Product product = new Product(productId,name,threshold,supplier);
        productDao.insert(product);
    }

    public void addBin(int binId,String locationCode) throws SQLException{
        Bin bin = new Bin(binId,locationCode);
        binDao.insert(bin);
    }

    public void addCustomer(int id,String name,String email) throws SQLException{
        Customer customer = new Customer(id,name,email);
        customerDao.insert(customer);
    }

    public void addSupplier(int id,String name,String email) throws SQLException{
        Supplier supplier = new Supplier(id,name,email);
        supplierDao.insert(supplier);
    }

    public void addStockItem(int productId,int binId,int quantity) throws SQLException{
        Product product = findProduct(productId);
        Bin bin = findBin(binId);

        if(product == null){
            throw new IllegalArgumentException("No product found with id: " + productId + ".");
        }
        if(bin == null){
            throw new IllegalArgumentException("No bin found with id: " + binId + ".");
        }
        StockItem stockItem = new StockItem(product,bin,quantity);
        stockItemDao.insert(stockItem);
    }

    public Product findProduct(int productId) throws SQLException {
        return productDao.findById(productId);
    }

    public Bin findBin(int binId) throws SQLException{
        return binDao.findById(binId);
    }

    public Supplier findSupplier(int supplierId) throws SQLException{
        return supplierDao.findById(supplierId);
    }

    public int checkStockLevel(int productId) throws SQLException{
        Product product = findProduct(productId);
        if(product == null) {
            throw new IllegalArgumentException("No product found with id: " + productId + ".");
        }
        List<StockItem> stockItemForProductId = stockItemDao.findByProduct(productId);
        int stockLevel = 0;

        for(StockItem item : stockItemForProductId){
            stockLevel += item.getQuantity();
        }

        return stockLevel;
    }


}
