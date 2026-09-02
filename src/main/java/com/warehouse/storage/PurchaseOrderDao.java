package com.warehouse.storage;

import com.warehouse.domain.PurchaseOrder;
import com.warehouse.domain.PurchaseOrderLineItem;


import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PurchaseOrderDao implements Repository<PurchaseOrder,Integer>{

    private Connection connection;
    private SupplierDao supplierDao;

    public PurchaseOrderDao(Connection connection,SupplierDao supplierDao){
        this.connection = connection;
        this.supplierDao = supplierDao;
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

    //Next up: findById, currently just a stub returning null. This is where you combine the two patterns you already used elsewhere:
    //
    //- Compose the Supplier — same as ProductDao.findById calling supplierDao.findById(supplierId).
    //- Build the List<PurchaseOrderLineItem> — same as StockItemDao.findByProduct() querying child rows into a list. Here: SELECT * FROM purchaseOrderLineItems WHERE purchaseOrderId = ?, and for each row, look up the Product via... think about what DAO you have access to for that, and what you don't (you don't have a ProductDao injected yet — does findById need one?).
    //
    //Go ahead and write it — post it when ready.

    @Override
    public PurchaseOrder findById(Integer integer) throws SQLException {
        return null;
    }

    @Override
    public List<PurchaseOrder> findAll() throws SQLException {
        return List.of();
    }

    @Override
    public void update(PurchaseOrder item) throws SQLException {

    }

    @Override
    public void delete(Integer integer) throws SQLException {

    }
}
