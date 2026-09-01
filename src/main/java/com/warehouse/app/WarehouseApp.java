package com.warehouse.app;

import com.warehouse.service.WarehouseService;
import com.warehouse.storage.*;
import com.warehouse.ui.WarehouseUI;

import java.sql.Connection;
import java.sql.SQLException;

public class WarehouseApp {

    public static void main(String[] args) throws SQLException {

        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.initTables();

        Connection connection = databaseManager.getConnection();

        SupplierDao supplierDao = new SupplierDao(connection);
        ProductDao productDao = new ProductDao(connection,supplierDao);
        BinDao binDao = new BinDao(connection);
        CustomerDao customerDao = new CustomerDao(connection);
        StockItemDao stockItemDao = new StockItemDao(connection,binDao,productDao);

        WarehouseService warehouseService = new WarehouseService(productDao,binDao,customerDao,supplierDao,stockItemDao);

        WarehouseUI warehouseUI = new WarehouseUI(warehouseService);
        warehouseUI.run();
    }
}
