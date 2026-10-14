package com.warehouse.storage;

import com.warehouse.domain.Customer;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KAN-92: an order is saved completely or not at all.
 * Needs KAN-90 (own test database) and KAN-91 (foreign keys) to trigger the failure.
 */
@Tag("integration")
@Epic("Orders")
@Feature("Data consistency")
@Issue("KAN-92")
@DisplayName("Order insert is atomic")
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
        db = TestDatabase.fresh("OrderAtomicityTest_" + info.getTestMethod().orElseThrow().getName());
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

    @Test
    @Disabled("KAN-92")
    @DisplayName("sales order: failing line item → no header and no line items stored")
    void salesOrderRolledBack() throws Exception {
        SalesOrder order = new SalesOrder(customer, List.of(
                new SalesOrderLineItem(product, 1),          // valid, inserted first
                new SalesOrderLineItem(unknownProduct, 1))); // fails

        assertThrows(SQLException.class, () -> salesOrderDao.insert(order));

        assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrders"), "orphan header");
        assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems"), "partial line items");
    }

    @Test
    @Disabled("KAN-92")
    @DisplayName("purchase order: failing line item → no header and no line items stored")
    void purchaseOrderRolledBack() throws Exception {
        PurchaseOrder order = new PurchaseOrder(supplier, List.of(
                new PurchaseOrderLineItem(product, 1),
                new PurchaseOrderLineItem(unknownProduct, 1)));

        assertThrows(SQLException.class, () -> purchaseOrderDao.insert(order));

        assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrders"), "orphan header");
        assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems"), "partial line items");
    }

    @Test
    @Disabled("KAN-92")
    @DisplayName("after a failed insert the next valid order is still stored normally")
    void connectionUsableAfterFailure() throws Exception {
        SalesOrder bad = new SalesOrder(customer, List.of(new SalesOrderLineItem(unknownProduct, 1)));
        assertThrows(SQLException.class, () -> salesOrderDao.insert(bad));

        int id = salesOrderDao.insert(new SalesOrder(customer, List.of(new SalesOrderLineItem(product, 2))));

        assertEquals(1, db.queryInt("SELECT COUNT(*) FROM salesOrders"));
        assertEquals(1, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems WHERE salesOrderId = ?", id));
    }
}
