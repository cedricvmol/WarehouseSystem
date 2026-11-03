package com.warehouse.storage;

import com.warehouse.domain.Bin;
import com.warehouse.domain.Customer;
import com.warehouse.domain.Product;
import com.warehouse.domain.Supplier;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KAN-95: findAll() of the master data DAOs against a real SQLite file.
 * Rows are inserted with plain SQL so the test does not depend on insert();
 * the expected values come from the tables, not from the DAO under test.
 * findAll() has no ORDER BY, so the tests do not assume an order.
 */
@Tag("integration")
@Epic("Storage")
@Feature("Master data")
@DisplayName("Master data DAOs - findAll (KAN-95)")
class MasterDataFindAllTest {

    private TestDatabase db;
    private SupplierDao supplierDao;
    private ProductDao productDao;
    private CustomerDao customerDao;
    private BinDao binDao;

    @BeforeEach
    void setUp(TestInfo info) throws Exception {
        String name = info.getTestClass().orElseThrow().getSimpleName() + "_"
                + info.getTestMethod().orElseThrow().getName();
        db = TestDatabase.fresh("MasterDataFindAllTest_" + name);
        supplierDao = new SupplierDao(db.connection);
        productDao = new ProductDao(db.connection, supplierDao);
        customerDao = new CustomerDao(db.connection);
        binDao = new BinDao(db.connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    private static <T> Map<Integer, T> byId(List<T> items, Function<T, Integer> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    @Nested
    @Story("List products")
    @DisplayName("ProductDao.findAll")
    class Products {

        @Test
        @DisplayName("Returns every product with its fields and its supplier loaded")
        void returnsEveryProduct() throws Exception {
            db.execute("INSERT INTO suppliers (supplierId, name, email) VALUES "
                    + "(1, 'Acme Supplies', 'orders@acme.test'), (2, 'Box Co', 'sales@boxco.test')");
            db.execute("INSERT INTO products (productId, name, threshold, supplierId) VALUES "
                    + "(101, 'Pallet wrap film', 10, 1), (102, 'Cardboard box L', 20, 2), (103, 'Packing tape', 15, 1)");

            Map<Integer, Product> products = byId(productDao.findAll(), Product::getProductId);

            assertEquals(db.queryInt("SELECT COUNT(*) FROM products"), products.size());
            Product box = products.get(102);
            assertAll(
                    () -> assertEquals("Cardboard box L", box.getName()),
                    () -> assertEquals(20, box.getThreshold()),
                    () -> assertEquals(2, box.getSupplier().getSupplierId()),
                    () -> assertEquals("Box Co", box.getSupplier().getName()),
                    () -> assertEquals("Acme Supplies", products.get(101).getSupplier().getName())
            );
        }

        @Test
        @DisplayName("Empty table gives an empty list")
        void emptyTable() throws Exception {
            assertTrue(productDao.findAll().isEmpty());
        }
    }

    @Nested
    @Story("List suppliers")
    @DisplayName("SupplierDao.findAll")
    class Suppliers {

        @Test
        @DisplayName("Returns every supplier with ID, name and email")
        void returnsEverySupplier() throws Exception {
            db.execute("INSERT INTO suppliers (supplierId, name, email) VALUES "
                    + "(1, 'Acme Supplies', 'orders@acme.test'), (2, 'Box Co', 'sales@boxco.test')");

            Map<Integer, Supplier> suppliers = byId(supplierDao.findAll(), Supplier::getSupplierId);

            assertEquals(2, suppliers.size());
            assertAll(
                    () -> assertEquals("Box Co", suppliers.get(2).getName()),
                    () -> assertEquals("sales@boxco.test", suppliers.get(2).getEmail()),
                    () -> assertEquals("Acme Supplies", suppliers.get(1).getName())
            );
        }

        @Test
        @DisplayName("Empty table gives an empty list")
        void emptyTable() throws Exception {
            assertTrue(supplierDao.findAll().isEmpty());
        }
    }

    @Nested
    @Story("List customers")
    @DisplayName("CustomerDao.findAll")
    class Customers {

        @Test
        @DisplayName("Returns every customer with ID, name and email")
        void returnsEveryCustomer() throws Exception {
            db.execute("INSERT INTO customers (customerId, name, email) VALUES "
                    + "(1, 'Globex Retail', 'purchasing@globex.test'), (2, 'Initech', 'buy@initech.test')");

            Map<Integer, Customer> customers = byId(customerDao.findAll(), Customer::getCustomerId);

            assertEquals(2, customers.size());
            assertAll(
                    () -> assertEquals("Initech", customers.get(2).getName()),
                    () -> assertEquals("buy@initech.test", customers.get(2).getEmail()),
                    () -> assertEquals("Globex Retail", customers.get(1).getName())
            );
        }

        @Test
        @DisplayName("Empty table gives an empty list")
        void emptyTable() throws Exception {
            assertTrue(customerDao.findAll().isEmpty());
        }
    }

    @Nested
    @Story("List bins")
    @DisplayName("BinDao.findAll")
    class Bins {

        @Test
        @DisplayName("Returns every bin with ID and location code")
        void returnsEveryBin() throws Exception {
            db.execute("INSERT INTO bins (binId, locationCode) VALUES (1, 'A-01-01'), (2, 'A-01-02')");

            Map<Integer, Bin> bins = byId(binDao.findAll(), Bin::getBinId);

            assertEquals(2, bins.size());
            assertAll(
                    () -> assertEquals("A-01-01", bins.get(1).getLocationCode()),
                    () -> assertEquals("A-01-02", bins.get(2).getLocationCode())
            );
        }

        @Test
        @DisplayName("Empty table gives an empty list")
        void emptyTable() throws Exception {
            assertTrue(binDao.findAll().isEmpty());
        }
    }
}
