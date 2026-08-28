package com.warehouse.service;

import com.warehouse.domain.Bin;
import com.warehouse.domain.Customer;
import com.warehouse.domain.Product;
import com.warehouse.domain.Supplier;
import com.warehouse.storage.*;

import java.sql.SQLException;

public class WarehouseService {

    private ProductDao productDao;
    private BinDao binDao;
    private CustomerDao customerDao;
    private SupplierDao supplierDao;
    private StockItemDao stockItemDao;

    public WarehouseService(ProductDao productDao,BinDao binDao,CustomerDao customerDao,SupplierDao supplierDao){
        this.productDao = productDao;
        this.binDao = binDao;
        this.customerDao = customerDao;
        this.supplierDao = supplierDao;
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


}
