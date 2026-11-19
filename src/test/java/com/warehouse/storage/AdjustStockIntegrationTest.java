package com.warehouse.storage;

import com.warehouse.domain.Bin;
import com.warehouse.domain.Product;
import com.warehouse.domain.Supplier;
import com.warehouse.service.WarehouseService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KAN-99 adjust stock end-to-end below the CLI: real service + DAOs on a fresh
 * SQLite file, every acceptance criterion verified with SQL on stockItems.
 *
 * Given for every test: product 1 lies in bin 1 with quantity 10, and in bin 2
 * with quantity 4 (to prove only the targeted row changes). Bin 3 exists but
 * holds nothing.
 */
@Tag("integration")
@Epic("Stock")
@Feature("Stock")
@Story("Adjust stock")
@DisplayName("Adjust stock (KAN-99, database)")
class AdjustStockIntegrationTest {

    private TestDatabase db;
    private WarehouseService service;

    @BeforeEach
    void setUp(TestInfo info) throws Exception {
        db = TestDatabase.fresh("AdjustStockIntegrationTest_" + info.getTestMethod().orElseThrow().getName());
        SupplierDao supplierDao = new SupplierDao(db.connection);
        ProductDao productDao = new ProductDao(db.connection, supplierDao);
        BinDao binDao = new BinDao(db.connection);
        CustomerDao customerDao = new CustomerDao(db.connection);
        StockItemDao stockItemDao = new StockItemDao(db.connection, binDao, productDao);
        service = new WarehouseService(db.connection, productDao, binDao, customerDao, supplierDao, stockItemDao,
                new PurchaseOrderDao(db.connection, supplierDao, productDao),
                new SalesOrderDao(db.connection, productDao, customerDao));

        Supplier supplier = new Supplier(1, "Parts Inc", "parts@example.com");
        supplierDao.insert(supplier);
        productDao.insert(new Product(1, "Widget", 5, supplier));
        binDao.insert(new Bin(1, "A1"));
        binDao.insert(new Bin(2, "A2"));
        binDao.insert(new Bin(3, "A3"));
        service.addStockItem(1, 1, 10);
        service.addStockItem(1, 2, 4);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    private int quantity(int productId, int binId) throws Exception {
        return db.queryInt("SELECT quantity FROM stockItems WHERE productId = ? AND binId = ?", productId, binId);
    }

    private int rows(int productId, int binId) throws Exception {
        return db.queryInt("SELECT COUNT(*) FROM stockItems WHERE productId = ? AND binId = ?", productId, binId);
    }

    private void assertUnchanged() throws Exception {
        assertAll(
                () -> assertEquals(2, db.queryInt("SELECT COUNT(*) FROM stockItems")),
                () -> assertEquals(10, quantity(1, 1)),
                () -> assertEquals(4, quantity(1, 2))
        );
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AC1 - adjust 10 -> 8: bin holds 8, other bin untouched")
    void adjustDown() throws Exception {
        service.adjustStock(1, 1, 8);

        assertEquals(8, quantity(1, 1));
        assertEquals(4, quantity(1, 2));
        assertEquals(1, rows(1, 1));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AC2 - adjust 10 -> 15: bin holds 15 (replaces, not adds), other bin untouched")
    void adjustUp() throws Exception {
        service.adjustStock(1, 1, 15);

        assertEquals(15, quantity(1, 1));
        assertEquals(4, quantity(1, 2));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AC3 - adjust to 0: stock row is removed, other bin untouched")
    void adjustToZeroRemovesRow() throws Exception {
        service.adjustStock(1, 1, 0);

        assertEquals(0, rows(1, 1));
        assertEquals(4, quantity(1, 2));
        assertEquals(4, service.checkStockLevel(1));
    }

    @Test
    @DisplayName("AC4 - negative quantity: error, nothing changes")
    void negativeRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.adjustStock(1, 1, -1));

        assertUnchanged();
    }

    @Test
    @DisplayName("AC5 - product not in this bin: error, no row created")
    void notInBinRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.adjustStock(1, 3, 5));

        assertEquals(0, rows(1, 3));
        assertUnchanged();
    }

    @Test
    @DisplayName("AC6 - unknown product: error, nothing changes")
    void unknownProductRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.adjustStock(999, 1, 5));

        assertUnchanged();
    }

    @Test
    @DisplayName("AC6 - unknown bin: error, nothing changes")
    void unknownBinRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.adjustStock(1, 999, 5));

        assertUnchanged();
    }
}
