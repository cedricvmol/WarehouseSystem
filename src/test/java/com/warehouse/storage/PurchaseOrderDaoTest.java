package com.warehouse.storage;

import com.warehouse.domain.OrderStatus;
import com.warehouse.domain.Product;
import com.warehouse.domain.PurchaseOrder;
import com.warehouse.domain.PurchaseOrderLineItem;
import com.warehouse.domain.Supplier;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Purchase order persistence against a real SQLite database, verified with SQL.
 * Mirror of {@link SalesOrderDaoTest}; existing behaviour (regression).
 */
@Tag("integration")
@Epic("Orders")
@Feature("Purchase order")
@DisplayName("PurchaseOrderDao (database)")
class PurchaseOrderDaoTest {

    private TestDatabase db;
    private PurchaseOrderDao purchaseOrderDao;

    private final Supplier supplier = new Supplier(1, "Parts Inc", "parts@example.com");
    private final Product widget = new Product(1, "Widget", 5, supplier);
    private final Product gadget = new Product(2, "Gadget", 5, supplier);

    @BeforeEach
    void setUp(TestInfo info) throws Exception {
        db = TestDatabase.fresh("PurchaseOrderDaoTest_" + info.getTestMethod().orElseThrow().getName());
        SupplierDao supplierDao = new SupplierDao(db.connection);
        ProductDao productDao = new ProductDao(db.connection, supplierDao);
        purchaseOrderDao = new PurchaseOrderDao(db.connection, supplierDao, productDao);

        supplierDao.insert(supplier);
        productDao.insert(widget);
        productDao.insert(gadget);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    private int insertOrder() throws Exception {
        return purchaseOrderDao.insert(new PurchaseOrder(supplier, List.of(
                new PurchaseOrderLineItem(widget, 3),
                new PurchaseOrderLineItem(gadget, 7))));
    }

    @Test
    @DisplayName("insert stores the order header as OPEN for the supplier")
    void insertStoresHeader() throws Exception {
        int id = insertOrder();

        assertEquals(1, db.queryInt("SELECT COUNT(*) FROM purchaseOrders"));
        assertEquals(1, db.queryInt("SELECT supplierId FROM purchaseOrders WHERE purchaseOrderId = ?", id));
        assertEquals("OPEN", db.queryString("SELECT orderStatus FROM purchaseOrders WHERE purchaseOrderId = ?", id));
    }

    @Test
    @DisplayName("insert stores every line item with its quantity")
    void insertStoresLineItems() throws Exception {
        int id = insertOrder();

        assertEquals(2, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems WHERE purchaseOrderId = ?", id));
        assertEquals(3, db.queryInt(
                "SELECT quantity FROM purchaseOrderLineItems WHERE purchaseOrderId = ? AND productId = 1", id));
        assertEquals(7, db.queryInt(
                "SELECT quantity FROM purchaseOrderLineItems WHERE purchaseOrderId = ? AND productId = 2", id));
    }

    @Test
    @DisplayName("insert generates a new id per order")
    void insertGeneratesIds() throws Exception {
        int first = insertOrder();
        int second = insertOrder();

        assertNotEquals(first, second);
        assertEquals(2, db.queryInt("SELECT COUNT(*) FROM purchaseOrders"));
    }

    @Test
    @DisplayName("same product twice in one order is refused by the primary key and nothing is stored")
    void duplicateProductRejected() {
        PurchaseOrder order = new PurchaseOrder(supplier, List.of(
                new PurchaseOrderLineItem(widget, 3),
                new PurchaseOrderLineItem(widget, 5)));

        assertThrows(SQLException.class, () -> purchaseOrderDao.insert(order));
        assertAll(
                () -> assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrders")),
                () -> assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems"))
        );
    }

    @Test
    @DisplayName("findById returns what was stored")
    void findByIdRoundTrip() throws Exception {
        int id = insertOrder();

        PurchaseOrder found = purchaseOrderDao.findById(id);

        assertEquals(id, found.getPurchaseOrderId());
        assertEquals(supplier.getSupplierId(), found.getSupplier().getSupplierId());
        assertEquals(OrderStatus.OPEN, found.getOrderStatus());
        assertEquals(2, found.getPurchaseOrderLineItems().size());
    }

    @Test
    @DisplayName("findById with an unknown id is rejected")
    void findByIdUnknown() {
        assertThrows(IllegalArgumentException.class, () -> purchaseOrderDao.findById(99));
    }

    @Test
    @DisplayName("update after cancel stores status CANCELLED and keeps the line items")
    void updateCancelled() throws Exception {
        int id = insertOrder();
        PurchaseOrder order = purchaseOrderDao.findById(id);

        order.cancel();
        purchaseOrderDao.update(order);

        assertEquals("CANCELLED", db.queryString("SELECT orderStatus FROM purchaseOrders WHERE purchaseOrderId = ?", id));
        assertEquals(2, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems WHERE purchaseOrderId = ?", id));
    }

    @Test
    @DisplayName("delete removes the header and its line items")
    void deleteRemovesEverything() throws Exception {
        int id = insertOrder();

        purchaseOrderDao.delete(id);

        assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrders WHERE purchaseOrderId = ?", id));
        assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems WHERE purchaseOrderId = ?", id));
    }
}
