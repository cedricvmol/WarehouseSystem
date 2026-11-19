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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KAN-98 transfer stock end-to-end below the CLI: real service + DAOs on a fresh
 * SQLite file, every acceptance criterion verified with SQL on stockItems.
 *
 * Given for every test: product 1 lies in bin 1 (A) with quantity 10 and in
 * bin 2 (B) with quantity 2. Bin 3 exists but holds nothing. Total = 12.
 */
@Tag("integration")
@Epic("Stock")
@Feature("Stock")
@Story("Transfer stock")
@DisplayName("Transfer stock (KAN-98, database)")
class TransferStockIntegrationTest {

    private static final int TOTAL = 12;

    private TestDatabase db;
    private WarehouseService service;

    @BeforeEach
    void setUp(TestInfo info) throws Exception {
        db = TestDatabase.fresh("TransferStockIntegrationTest_" + info.getTestMethod().orElseThrow().getName()
                + "_" + Math.abs(info.getDisplayName().hashCode()));
        SupplierDao supplierDao = new SupplierDao(db.connection);
        ProductDao productDao = new ProductDao(db.connection, supplierDao);
        BinDao binDao = new BinDao(db.connection);
        CustomerDao customerDao = new CustomerDao(db.connection);
        StockItemDao stockItemDao = new StockItemDao(db.connection, binDao, productDao);
        service = new WarehouseService(productDao, binDao, customerDao, supplierDao, stockItemDao,
                new PurchaseOrderDao(db.connection, supplierDao, productDao),
                new SalesOrderDao(db.connection, productDao, customerDao));

        Supplier supplier = new Supplier(1, "Parts Inc", "parts@example.com");
        supplierDao.insert(supplier);
        productDao.insert(new Product(1, "Widget", 5, supplier));
        binDao.insert(new Bin(1, "A1"));
        binDao.insert(new Bin(2, "A2"));
        binDao.insert(new Bin(3, "A3"));
        service.addStockItem(1, 1, 10);
        service.addStockItem(1, 2, 2);
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

    private int totalOfProduct1() throws Exception {
        return db.queryInt("SELECT COALESCE(SUM(quantity), 0) FROM stockItems WHERE productId = 1");
    }

    private void assertUnchanged() throws Exception {
        assertAll(
                () -> assertEquals(2, db.queryInt("SELECT COUNT(*) FROM stockItems")),
                () -> assertEquals(10, quantity(1, 1)),
                () -> assertEquals(2, quantity(1, 2))
        );
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AC1 - A=10, B=2, move 4 from A to B: A holds 6, B holds 6")
    void moveBetweenBinsThatBothHoldProduct() throws Exception {
        service.transferStock(1, 1, 2, 4);

        assertAll(
                () -> assertEquals(6, quantity(1, 1)),
                () -> assertEquals(6, quantity(1, 2))
        );
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AC2 - move 4 from A to empty bin 3: row P/3 created with 4, A holds 6")
    void moveToBinWithoutProductCreatesRow() throws Exception {
        service.transferStock(1, 1, 3, 4);

        assertAll(
                () -> assertEquals(1, rows(1, 3)),
                () -> assertEquals(4, quantity(1, 3)),
                () -> assertEquals(6, quantity(1, 1))
        );
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AC3 - move all 10 from A to B: row P/A removed, B holds 12")
    void moveAllRemovesSourceRow() throws Exception {
        service.transferStock(1, 1, 2, 10);

        assertAll(
                () -> assertEquals(0, rows(1, 1)),
                () -> assertEquals(12, quantity(1, 2))
        );
    }

    @Test
    @DisplayName("AC2+AC3 - move all 10 from A to empty bin 3: P/A removed, P/3 created with 10")
    void moveAllToEmptyBin() throws Exception {
        service.transferStock(1, 1, 3, 10);

        assertAll(
                () -> assertEquals(0, rows(1, 1)),
                () -> assertEquals(10, quantity(1, 3)),
                () -> assertEquals(2, quantity(1, 2))
        );
    }

    @ParameterizedTest(name = "move {2} from bin {0} to bin {1}")
    @CsvSource({"1, 2, 4", "1, 3, 4", "1, 2, 10", "1, 3, 10", "2, 1, 2", "2, 3, 1"})
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AC4 - after any successful transfer the total stock of P is unchanged (12)")
    void totalUnchanged(int from, int to, int quantity) throws Exception {
        service.transferStock(1, from, to, quantity);

        assertEquals(TOTAL, totalOfProduct1());
        assertEquals(TOTAL, service.checkStockLevel(1));
    }

    @Test
    @DisplayName("AC5 - move 11 while A holds 10: error, nothing changes")
    void moreThanAvailableRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.transferStock(1, 1, 2, 11));

        assertUnchanged();
    }

    @ParameterizedTest(name = "quantity {0}")
    @ValueSource(ints = {0, -1})
    @DisplayName("AC6 - quantity 0 or less: error, nothing changes")
    void nonPositiveQuantityRejected(int quantity) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.transferStock(1, 1, 2, quantity));

        assertUnchanged();
    }

    @Test
    @DisplayName("AC7 - source and target are the same bin: error, nothing changes")
    void sameBinRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.transferStock(1, 1, 1, 4));

        assertUnchanged();
    }

    @Test
    @DisplayName("AC8 - P not in source bin 3: error, no row created in target")
    void productNotInSourceBinRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.transferStock(1, 3, 2, 1));

        assertEquals(0, rows(1, 3));
        assertUnchanged();
    }

    @Test
    @DisplayName("AC8 - source bin does not exist: error, nothing changes")
    void unknownSourceBinRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.transferStock(1, 999, 2, 1));

        assertUnchanged();
    }

    @Test
    @DisplayName("AC8 - target bin does not exist: error, nothing changes (source not decreased)")
    void unknownTargetBinRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.transferStock(1, 1, 999, 4));

        assertUnchanged();
    }

    @Test
    @DisplayName("AC8 - unknown product: error, nothing changes")
    void unknownProductRejected() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.transferStock(999, 1, 2, 4));

        assertUnchanged();
    }
}
