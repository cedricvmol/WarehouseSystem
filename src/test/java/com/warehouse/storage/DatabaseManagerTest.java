package com.warehouse.storage;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@Epic("Storage")
@Feature("Database setup")
@Issue("KAN-90")
@DisplayName("DatabaseManager (configurable URL)")
class DatabaseManagerTest {

    private static final Path PRODUCTION_DB = Path.of("warehouse.db");

    private static final List<String> TABLES = List.of(
            "suppliers", "products", "bins", "customers", "stockItems",
            "purchaseOrders", "purchaseOrderLineItems", "salesOrders", "salesOrderLineItems");

    private boolean productionDbExisted;
    private long productionDbModified;
    private TestDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        productionDbExisted = Files.exists(PRODUCTION_DB);
        productionDbModified = productionDbExisted ? Files.getLastModifiedTime(PRODUCTION_DB).toMillis() : 0;
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db != null) {
            db.close();
        }
    }

    @Test
    @Disabled("KAN-90")
    @DisplayName("initTables() creates the database file at the given URL")
    void createsFileAtGivenUrl(TestInfo info) throws Exception {
        db = TestDatabase.fresh(info.getTestMethod().orElseThrow().getName());

        assertTrue(Files.exists(db.file), "expected database file " + db.file);
    }

    @Test
    @Disabled("KAN-90")
    @DisplayName("initTables() creates all tables in the given database")
    void createsAllTables(TestInfo info) throws Exception {
        db = TestDatabase.fresh(info.getTestMethod().orElseThrow().getName());

        for (String table : TABLES) {
            int found = db.queryInt("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?", table);
            assertEquals(1, found, "table " + table);
        }
    }

    @Test
    @Disabled("KAN-90")
    @DisplayName("The real warehouse.db is not created or modified")
    void doesNotTouchProductionDatabase(TestInfo info) throws Exception {
        db = TestDatabase.fresh(info.getTestMethod().orElseThrow().getName());
        db.execute(
                "INSERT INTO customers (customerId, name, email) VALUES (1, 'Acme', 'acme@example.com')");

        assertEquals(productionDbExisted, Files.exists(PRODUCTION_DB), "warehouse.db existence changed");
        if (productionDbExisted) {
            assertEquals(productionDbModified, Files.getLastModifiedTime(PRODUCTION_DB).toMillis(),
                    "warehouse.db was modified");
        }
    }

    @Test
    @Disabled("KAN-90")
    @DisplayName("Two databases with different URLs are independent")
    void databasesAreIsolated(TestInfo info) throws Exception {
        String name = info.getTestMethod().orElseThrow().getName();
        db = TestDatabase.fresh(name + "_a");
        try (TestDatabase other = TestDatabase.fresh(name + "_b")) {
            db.execute(
                    "INSERT INTO customers (customerId, name, email) VALUES (1, 'Acme', 'acme@example.com')");

            assertEquals(1, db.queryInt("SELECT COUNT(*) FROM customers"));
            assertEquals(0, other.queryInt("SELECT COUNT(*) FROM customers"));
        }
    }
}
