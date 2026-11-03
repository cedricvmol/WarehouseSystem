package com.warehouse.service;

import com.warehouse.domain.*;
import com.warehouse.storage.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WarehouseService {

    private ProductDao productDao;
    private BinDao binDao;
    private CustomerDao customerDao;
    private SupplierDao supplierDao;
    private StockItemDao stockItemDao;
    private PurchaseOrderDao purchaseOrderDao;
    private SalesOrderDao salesOrderDao;

    public WarehouseService(ProductDao productDao,BinDao binDao,CustomerDao customerDao,SupplierDao supplierDao,StockItemDao stockItemDao,PurchaseOrderDao purchaseOrderDao, SalesOrderDao salesOrderDao){
        this.productDao = productDao;
        this.binDao = binDao;
        this.customerDao = customerDao;
        this.supplierDao = supplierDao;
        this.stockItemDao = stockItemDao;
        this.purchaseOrderDao = purchaseOrderDao;
        this.salesOrderDao = salesOrderDao;
    }

    public void addProduct(int productId,String name, int threshold,int supplierId) throws SQLException {
        if(productDao.findById(productId).isPresent()){
            throw new IllegalArgumentException("A product with id "+ productId +" already exists.");
        }

        Supplier supplier = findSupplier(supplierId);
        Product product = new Product(productId,name,threshold,supplier);
        productDao.insert(product);
    }

    public void addBin(int binId,String locationCode) throws SQLException{
        if(binDao.findById(binId).isPresent()){
            throw new IllegalArgumentException("A bin with id "+ binId + " already exists.");
        }
        Bin bin = new Bin(binId,locationCode);
        binDao.insert(bin);
    }

    public void addCustomer(int id,String name,String email) throws SQLException{
        if(customerDao.findById(id).isPresent()){
            throw new IllegalArgumentException("A customer with id "+ id + " already exists.");
        }
        Customer customer = new Customer(id,name,email);
        customerDao.insert(customer);
    }

    public void addSupplier(int id,String name,String email) throws SQLException{
        if(supplierDao.findById(id).isPresent()){
            throw new IllegalArgumentException("A supplier with id "+ id + " already exists.");
        }
        Supplier supplier = new Supplier(id,name,email);
        supplierDao.insert(supplier);
    }

    public void addStockItem(int productId,int binId,int quantity) throws SQLException{
        Product product = findProduct(productId);
        Bin bin = findBin(binId);
        StockItem stockItem = new StockItem(product,bin,quantity);
        stockItemDao.insert(stockItem);
    }

    public Product findProduct(int productId) throws SQLException {
        return productDao.findById(productId).orElseThrow(() -> new IllegalArgumentException("No product found with id: "+ productId + "."));
    }

    public Bin findBin(int binId) throws SQLException{
        return binDao.findById(binId).orElseThrow(()-> new IllegalArgumentException("No bin found with id: "+ binId + "."));
    }

    public Supplier findSupplier(int supplierId) throws SQLException{
        return supplierDao.findById(supplierId).orElseThrow(()-> new IllegalArgumentException("No supplier found with id: "+ supplierId + "."));
    }

    public Customer findCustomer(int customerId) throws SQLException{
        return customerDao.findById(customerId).orElseThrow(()-> new IllegalArgumentException("No customer found with id: "+ customerId + "."));
    }

    public int checkStockLevel(int productId) throws SQLException{
        findProduct(productId);
        List<StockItem> stockItemForProductId = stockItemDao.findByProduct(productId);
        int stockLevel = 0;

        for(StockItem item : stockItemForProductId){
            stockLevel += item.getQuantity();
        }

        return stockLevel;
    }

    public int createPurchaseOrder(int supplierId, Map<Integer,Integer> lineItems) throws SQLException {
        if(lineItems == null){
            throw new IllegalArgumentException("No line items were given for the purchase order.");
        }
        List<PurchaseOrderLineItem> purchaseOrderLineItems = new ArrayList<>();
        for(Map.Entry<Integer,Integer> item : lineItems.entrySet())
        {
            Product product = findProduct(item.getKey());
            PurchaseOrderLineItem purchaseOrderLineItem = new PurchaseOrderLineItem(product,item.getValue());
            purchaseOrderLineItems.add(purchaseOrderLineItem);
        }

        Supplier supplier = findSupplier(supplierId);
        PurchaseOrder purchaseOrder = new PurchaseOrder(supplier,purchaseOrderLineItems);
        return purchaseOrderDao.insert(purchaseOrder);
    }

    public int createSalesOrder(int customerId,Map<Integer,Integer> lineItems) throws SQLException{
        if(lineItems == null){
            throw new IllegalArgumentException("No line items were given for the sales order.");
        }
        List<SalesOrderLineItem> salesOrderLineItems = new ArrayList<>();
        for(Map.Entry<Integer,Integer> item : lineItems.entrySet()){
            Product product = findProduct(item.getKey());
            SalesOrderLineItem salesOrderLineItem = new SalesOrderLineItem(product,item.getValue());
            salesOrderLineItems.add(salesOrderLineItem);
        }

        Customer customer = findCustomer(customerId);
        SalesOrder salesOrder = new SalesOrder(customer,salesOrderLineItems);
        return salesOrderDao.insert(salesOrder);
    }


    public void cancelPurchaseOrder(int purchaseOrderId) throws SQLException {
        PurchaseOrder purchaseOrder = purchaseOrderDao.findById(purchaseOrderId);
        purchaseOrder.cancel();
        purchaseOrderDao.update(purchaseOrder);
    }

    public void cancelSalesOrder(int salesOrderId) throws SQLException {
        SalesOrder salesOrder = salesOrderDao.findById(salesOrderId);
        salesOrder.cancel();
        salesOrderDao.update(salesOrder);
    }

    public List<Product> giveListAllProducts() throws SQLException{
        return productDao.findAll();
    }

    public List<Supplier> giveListAllSuppliers() throws SQLException{
        return supplierDao.findAll();
    }

    public List<Customer> giveListAllCustomers() throws SQLException{
        return customerDao.findAll();
    }

    public List<Bin> giveListAllBins() throws SQLException{
        return binDao.findAll();
    }


}
