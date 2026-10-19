package com.warehouse.storage;

import com.warehouse.domain.Customer;
import com.warehouse.domain.OrderStatus;
import com.warehouse.domain.Product;
import com.warehouse.domain.PurchaseOrder;
import com.warehouse.domain.PurchaseOrderLineItem;
import com.warehouse.domain.SalesOrder;
import com.warehouse.domain.SalesOrderLineItem;
import com.warehouse.domain.Supplier;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An order is written completely or not at all.
 * KAN-92: insert. KAN-93: update and delete.
 * Failures are triggered with foreign keys (unknown product) or, for delete,
 * with a test-only trigger that blocks deleting the order header.
 */
@Tag("integration")
@Epic("Orders")
@Feature("Data consistency")
@DisplayName("Order writes are atomic")
class OrderAtomicityTest {

    private TestDatabase db;
    private SalesOrderDao salesOrderDao;
    private PurchaseOrderDao purchaseOrderDao;

    private final Supplier supplier = new Supplier(1, "Parts Inc", "parts@example.com");
    private final Product product = new Product(1, "Widget", 5, supplier);
    private final Customer customer = new Customer(1, "Acme", "acme@example.com");
    private final Product unknownProduct = new Product(99, "Ghost", 5, supplier);

    @BeforeEach
    void setUp(TestInfo info) throws Exception {
        String testClass = info.getTestClass().orElseThrow().getSimpleName();
        String testMethod = info.getTestMethod().orElseThrow().getName();
        db = TestDatabase.fresh("OrderAtomicityTest_" + testClass + "_" + testMethod);
        SupplierDao supplierDao = new SupplierDao(db.connection);
        ProductDao productDao = new ProductDao(db.connection, supplierDao);
        CustomerDao customerDao = new CustomerDao(db.connection);
        salesOrderDao = new SalesOrderDao(db.connection, productDao, customerDao);
        purchaseOrderDao = new PurchaseOrderDao(db.connection, supplierDao, productDao);

        supplierDao.insert(supplier);
        productDao.insert(product);
        customerDao.insert(customer);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    /** Stores an OPEN sales order with one line item: product x 2. */
    private int existingSalesOrder() throws SQLException {
        return salesOrderDao.insert(new SalesOrder(customer, List.of(new SalesOrderLineItem(product, 2))));
    }

    /** Stores an OPEN purchase order with one line item: product x 2. */
    private int existingPurchaseOrder() throws SQLException {
        return purchaseOrderDao.insert(new PurchaseOrder(supplier, List.of(new PurchaseOrderLineItem(product, 2))));
    }

    @Nested
    @Issue("KAN-92")
    @DisplayName("insert")
    class Insert {

        @Test
        @DisplayName("sales order: failing line item → no header and no line items stored")
        void salesOrderRolledBack() {
            SalesOrder order = new SalesOrder(customer, List.of(
                    new SalesOrderLineItem(product, 1),          // valid, inserted first
                    new SalesOrderLineItem(unknownProduct, 1))); // fails

            assertThrows(SQLException.class, () -> salesOrderDao.insert(order));

            assertAll(
                    () -> assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrders"), "orphan header"),
                    () -> assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems"), "partial line items"));
        }

        @Test
        @DisplayName("purchase order: failing line item → no header and no line items stored")
        void purchaseOrderRolledBack() {
            PurchaseOrder order = new PurchaseOrder(supplier, List.of(
                    new PurchaseOrderLineItem(product, 1),
                    new PurchaseOrderLineItem(unknownProduct, 1)));

            assertThrows(SQLException.class, () -> purchaseOrderDao.insert(order));

            assertAll(
                    () -> assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrders"), "orphan header"),
                    () -> assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems"), "partial line items"));
        }

        @Test
        @DisplayName("after a failed insert the next valid order is still stored normally")
        void connectionUsableAfterFailure() throws Exception {
            SalesOrder bad = new SalesOrder(customer, List.of(new SalesOrderLineItem(unknownProduct, 1)));
            assertThrows(SQLException.class, () -> salesOrderDao.insert(bad));

            int id = existingSalesOrder();

            assertEquals(1, db.queryInt("SELECT COUNT(*) FROM salesOrders"));
            assertEquals(1, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems WHERE salesOrderId = ?", id));
        }
    }

    @Nested
    @Issue("KAN-93")
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("sales order: failing line item → header and line items unchanged")
        void salesOrderRolledBack() throws Exception {
            int id = existingSalesOrder();
            SalesOrder changed = new SalesOrder(id, customer, LocalDateTime.now(), OrderStatus.CANCELLED, List.of(
                    new SalesOrderLineItem(product, 5),
                    new SalesOrderLineItem(unknownProduct, 1))); // fails after header update + line item delete

            assertThrows(SQLException.class, () -> salesOrderDao.update(changed));

            assertAll(
                    () -> assertEquals("OPEN", db.queryString("SELECT orderStatus FROM salesOrders WHERE salesOrderId = ?", id), "header updated"),
                    () -> assertEquals(1, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems WHERE salesOrderId = ?", id), "line items lost"),
                    () -> assertEquals(2, db.queryInt("SELECT quantity FROM salesOrderLineItems WHERE salesOrderId = ?", id), "line item changed"));
        }

        @Test
        @DisplayName("purchase order: failing line item → header and line items unchanged")
        void purchaseOrderRolledBack() throws Exception {
            int id = existingPurchaseOrder();
            PurchaseOrder changed = new PurchaseOrder(id, supplier, LocalDateTime.now(), OrderStatus.CANCELLED, List.of(
                    new PurchaseOrderLineItem(product, 5),
                    new PurchaseOrderLineItem(unknownProduct, 1)));

            assertThrows(SQLException.class, () -> purchaseOrderDao.update(changed));

            assertAll(
                    () -> assertEquals("OPEN", db.queryString("SELECT orderStatus FROM purchaseOrders WHERE purchaseOrderId = ?", id), "header updated"),
                    () -> assertEquals(1, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems WHERE purchaseOrderId = ?", id), "line items lost"),
                    () -> assertEquals(2, db.queryInt("SELECT quantity FROM purchaseOrderLineItems WHERE purchaseOrderId = ?", id), "line item changed"));
        }

        @Test
        @DisplayName("sales order: valid update stores header and line items")
        void salesOrderValidUpdate() throws Exception {
            int id = existingSalesOrder();
            SalesOrder changed = new SalesOrder(id, customer, LocalDateTime.now(), OrderStatus.CANCELLED,
                    List.of(new SalesOrderLineItem(product, 7)));

            salesOrderDao.update(changed);

            assertAll(
                    () -> assertEquals("CANCELLED", db.queryString("SELECT orderStatus FROM salesOrders WHERE salesOrderId = ?", id)),
                    () -> assertEquals(1, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems WHERE salesOrderId = ?", id)),
                    () -> assertEquals(7, db.queryInt("SELECT quantity FROM salesOrderLineItems WHERE salesOrderId = ?", id)));
        }

        @Test
        @DisplayName("purchase order: valid update stores header and line items")
        void purchaseOrderValidUpdate() throws Exception {
            int id = existingPurchaseOrder();
            PurchaseOrder changed = new PurchaseOrder(id, supplier, LocalDateTime.now(), OrderStatus.CANCELLED,
                    List.of(new PurchaseOrderLineItem(product, 7)));

            purchaseOrderDao.update(changed);

            assertAll(
                    () -> assertEquals("CANCELLED", db.queryString("SELECT orderStatus FROM purchaseOrders WHERE purchaseOrderId = ?", id)),
                    () -> assertEquals(1, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems WHERE purchaseOrderId = ?", id)),
                    () -> assertEquals(7, db.queryInt("SELECT quantity FROM purchaseOrderLineItems WHERE purchaseOrderId = ?", id)));
        }

        @Test
        @DisplayName("after a failed update auto-commit is back on and the next update is stored")
        void connectionUsableAfterFailure() throws Exception {
            int id = existingSalesOrder();
            SalesOrder bad = new SalesOrder(id, customer, LocalDateTime.now(), OrderStatus.CANCELLED,
                    List.of(new SalesOrderLineItem(unknownProduct, 1)));
            assertThrows(SQLException.class, () -> salesOrderDao.update(bad));

            assertTrue(db.connection.getAutoCommit(), "auto-commit not restored");

            salesOrderDao.update(new SalesOrder(id, customer, LocalDateTime.now(), OrderStatus.CANCELLED,
                    List.of(new SalesOrderLineItem(product, 3))));
            assertEquals("CANCELLED", db.queryString("SELECT orderStatus FROM salesOrders WHERE salesOrderId = ?", id));
        }
    }

    @Nested
    @Issue("KAN-93")
    @DisplayName("delete")
    class Delete {

        /** Test-only fault injection: deleting an order header always fails. */
        private void blockHeaderDelete(String table) throws SQLException {
            db.execute("CREATE TRIGGER block_header_delete BEFORE DELETE ON " + table
                    + " BEGIN SELECT RAISE(ABORT, 'test: header delete blocked'); END");
        }

        @Test
        @DisplayName("sales order: failing header delete → header and line items still there")
        void salesOrderRolledBack() throws Exception {
            int id = existingSalesOrder();
            blockHeaderDelete("salesOrders");

            assertThrows(SQLException.class, () -> salesOrderDao.delete(id));

            assertAll(
                    () -> assertEquals(1, db.queryInt("SELECT COUNT(*) FROM salesOrders WHERE salesOrderId = ?", id), "header gone"),
                    () -> assertEquals(1, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems WHERE salesOrderId = ?", id), "line items lost"));
        }

        @Test
        @DisplayName("purchase order: failing header delete → header and line items still there")
        void purchaseOrderRolledBack() throws Exception {
            int id = existingPurchaseOrder();
            blockHeaderDelete("purchaseOrders");

            assertThrows(SQLException.class, () -> purchaseOrderDao.delete(id));

            assertAll(
                    () -> assertEquals(1, db.queryInt("SELECT COUNT(*) FROM purchaseOrders WHERE purchaseOrderId = ?", id), "header gone"),
                    () -> assertEquals(1, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems WHERE purchaseOrderId = ?", id), "line items lost"));
        }

        @Test
        @DisplayName("sales order: valid delete removes header and line items")
        void salesOrderValidDelete() throws Exception {
            int id = existingSalesOrder();

            salesOrderDao.delete(id);

            assertAll(
                    () -> assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrders")),
                    () -> assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems")));
        }

        @Test
        @DisplayName("purchase order: valid delete removes header and line items")
        void purchaseOrderValidDelete() throws Exception {
            int id = existingPurchaseOrder();

            purchaseOrderDao.delete(id);

            assertAll(
                    () -> assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrders")),
                    () -> assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems")));
        }

        @Test
        @DisplayName("after a failed delete auto-commit is back on and the next delete is stored")
        void connectionUsableAfterFailure() throws Exception {
            int id = existingSalesOrder();
            blockHeaderDelete("salesOrders");
            assertThrows(SQLException.class, () -> salesOrderDao.delete(id));

            assertTrue(db.connection.getAutoCommit(), "auto-commit not restored");

            db.execute("DROP TRIGGER block_header_delete");
            salesOrderDao.delete(id);
            assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrders"));
        }
    }
}
