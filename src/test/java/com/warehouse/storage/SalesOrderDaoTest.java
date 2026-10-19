package com.warehouse.storage;

import com.warehouse.domain.Customer;
import com.warehouse.domain.OrderStatus;
import com.warehouse.domain.Product;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sales order persistence against a real SQLite database, verified with SQL.
 * Existing behaviour (regression), only blocked by KAN-90 (own test database).
 */
@Tag("integration")
@Epic("Orders")
@Feature("Sales order")
@Issue("KAN-90")
@DisplayName("SalesOrderDao (database)")
class SalesOrderDaoTest {

    private TestDatabase db;
    private SalesOrderDao salesOrderDao;

    private final Supplier supplier = new Supplier(1, "Parts Inc", "parts@example.com");
    private final Product widget = new Product(1, "Widget", 5, supplier);
    private final Product gadget = new Product(2, "Gadget", 5, supplier);
    private final Customer customer = new Customer(1, "Acme", "acme@example.com");

    @BeforeEach
    void setUp(TestInfo info) throws Exception {
        db = TestDatabase.fresh("SalesOrderDaoTest_" + info.getTestMethod().orElseThrow().getName());
        SupplierDao supplierDao = new SupplierDao(db.connection);
        ProductDao productDao = new ProductDao(db.connection, supplierDao);
        CustomerDao customerDao = new CustomerDao(db.connection);
        salesOrderDao = new SalesOrderDao(db.connection, productDao, customerDao);

        supplierDao.insert(supplier);
        productDao.insert(widget);
        productDao.insert(gadget);
        customerDao.insert(customer);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    private int insertOrder() throws Exception {
        return salesOrderDao.insert(new SalesOrder(customer, List.of(
                new SalesOrderLineItem(widget, 3),
                new SalesOrderLineItem(gadget, 7))));
    }

    @Test
    @DisplayName("insert stores the order header as OPEN for the customer")
    void insertStoresHeader() throws Exception {
        int id = insertOrder();

        assertEquals(1, db.queryInt("SELECT COUNT(*) FROM salesOrders"));
        assertEquals(1, db.queryInt("SELECT customerId FROM salesOrders WHERE salesOrderId = ?", id));
        assertEquals("OPEN", db.queryString("SELECT orderStatus FROM salesOrders WHERE salesOrderId = ?", id));
    }

    @Test
    @DisplayName("insert stores every line item with its quantity")
    void insertStoresLineItems() throws Exception {
        int id = insertOrder();

        assertEquals(2, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems WHERE salesOrderId = ?", id));
        assertEquals(3, db.queryInt(
                "SELECT quantity FROM salesOrderLineItems WHERE salesOrderId = ? AND productId = 1", id));
        assertEquals(7, db.queryInt(
                "SELECT quantity FROM salesOrderLineItems WHERE salesOrderId = ? AND productId = 2", id));
    }

    @Test
    @DisplayName("insert generates a new id per order")
    void insertGeneratesIds() throws Exception {
        int first = insertOrder();
        int second = insertOrder();

        assertNotEquals(first, second);
        assertEquals(2, db.queryInt("SELECT COUNT(*) FROM salesOrders"));
    }

    @Test
    @DisplayName("findById returns what was stored")
    void findByIdRoundTrip() throws Exception {
        int id = insertOrder();

        SalesOrder found = salesOrderDao.findById(id);

        assertEquals(id, found.getSalesOrderId());
        assertEquals(customer.getCustomerId(), found.getCustomer().getCustomerId());
        assertEquals(OrderStatus.OPEN, found.getOrderStatus());
        assertEquals(2, found.getSalesOrderLineItems().size());
    }

    @Test
    @DisplayName("update after cancel stores status CANCELLED and keeps the line items")
    void updateCancelled() throws Exception {
        int id = insertOrder();
        SalesOrder order = salesOrderDao.findById(id);

        order.cancel();
        salesOrderDao.update(order);

        assertEquals("CANCELLED", db.queryString("SELECT orderStatus FROM salesOrders WHERE salesOrderId = ?", id));
        assertEquals(2, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems WHERE salesOrderId = ?", id));
    }

    @Test
    @DisplayName("delete removes the header and its line items")
    void deleteRemovesEverything() throws Exception {
        int id = insertOrder();

        salesOrderDao.delete(id);

        assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrders WHERE salesOrderId = ?", id));
        assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems WHERE salesOrderId = ?", id));
    }
}
