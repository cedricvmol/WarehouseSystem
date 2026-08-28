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

    public void addProduct(Product product) throws SQLException {
        productDao.insert(product);
    }

    public void addBin(Bin bin) throws SQLException{
        binDao.insert(bin);
    }

    public void addCustomer(Customer customer) throws SQLException{
        customerDao.insert(customer);
    }

    public void addSupplier(Supplier supplier) throws SQLException{
        supplierDao.insert(supplier);
    }

    public void addStockItem(StockItem stockItem) throws SQLException{
        stockItemDao.insert(stockItem);
    }

    public int checkStockLevel(int productId) throws SQLException{
        List<StockItem> stockItemForProductId = stockItemDao.findByProduct(productId);
        int stockLevel = 0;

        for(StockItem item : stockItemForProductId){
            stockLevel += item.getQuantity();
        }

        return stockLevel;
    }


}
