package com.warehouse.storage;

import com.warehouse.domain.Bin;
import com.warehouse.domain.Customer;
import com.warehouse.domain.Product;
import com.warehouse.domain.PurchaseOrder;
import com.warehouse.domain.PurchaseOrderLineItem;
import com.warehouse.domain.SalesOrder;
import com.warehouse.domain.SalesOrderLineItem;
import com.warehouse.domain.StockItem;
import com.warehouse.domain.Supplier;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KAN-91: SQLite only checks FOREIGN KEYs with {@code PRAGMA foreign_keys = ON}.
 * Also needs KAN-90 (own test database) before these can run.
 */
@Tag("integration")
@Epic("Storage")
@Feature("Referential integrity")
@Issue("KAN-91")
@DisplayName("Foreign keys are enforced")
class ForeignKeyTest {

    private TestDatabase db;
    private SupplierDao supplierDao;
    private ProductDao productDao;
    private BinDao binDao;
    private CustomerDao customerDao;
    private StockItemDao stockItemDao;
    private PurchaseOrderDao purchaseOrderDao;
    private SalesOrderDao salesOrderDao;

    // Present in the database
    private final Supplier supplier = new Supplier(1, "Parts Inc", "parts@example.com");
    private final Product product = new Product(1, "Widget", 5, supplier);
    private final Bin bin = new Bin(1, "A-01");
    private final Customer customer = new Customer(1, "Acme", "acme@example.com");

    // Never stored
    private final Supplier unknownSupplier = new Supplier(99, "Ghost Supplier", "ghost@example.com");
    private final Product unknownProduct = new Product(99, "Ghost", 5, supplier);
    private final Bin unknownBin = new Bin(99, "Z-99");
    private final Customer unknownCustomer = new Customer(99, "Ghost Customer", "ghost@example.com");

    @BeforeEach
    void setUp(TestInfo info) throws Exception {
        db = TestDatabase.fresh("ForeignKeyTest_" + info.getTestMethod().orElseThrow().getName());
        supplierDao = new SupplierDao(db.connection);
        productDao = new ProductDao(db.connection, supplierDao);
        binDao = new BinDao(db.connection);
        customerDao = new CustomerDao(db.connection);
        stockItemDao = new StockItemDao(db.connection, binDao, productDao);
        purchaseOrderDao = new PurchaseOrderDao(db.connection, supplierDao, productDao);
        salesOrderDao = new SalesOrderDao(db.connection, productDao, customerDao);

        supplierDao.insert(supplier);
        productDao.insert(product);
        binDao.insert(bin);
        customerDao.insert(customer);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.close();
    }

    @Test
    @Disabled("KAN-91")
    @DisplayName("PRAGMA foreign_keys is ON for the connection")
    void pragmaIsOn() throws Exception {
        assertEquals(1, db.queryInt("PRAGMA foreign_keys"));
    }

    @Nested
    @DisplayName("Insert with a non-existing parent is rejected")
    class InsertWithUnknownParent {

        @Test
        @Disabled("KAN-91")
        @DisplayName("product → unknown supplier")
        void productWithUnknownSupplier() throws Exception {
            Product orphan = new Product(2, "Orphan", 5, unknownSupplier);

            assertThrows(SQLException.class, () -> productDao.insert(orphan));
            assertEquals(0, db.queryInt("SELECT COUNT(*) FROM products WHERE productId = 2"));
        }

        @Test
        @Disabled("KAN-91")
        @DisplayName("stock item → unknown product")
        void stockItemWithUnknownProduct() throws Exception {
            assertThrows(SQLException.class, () -> stockItemDao.insert(new StockItem(unknownProduct, bin, 10)));
            assertEquals(0, db.queryInt("SELECT COUNT(*) FROM stockItems"));
        }

        @Test
        @Disabled("KAN-91")
        @DisplayName("stock item → unknown bin")
        void stockItemWithUnknownBin() throws Exception {
            assertThrows(SQLException.class, () -> stockItemDao.insert(new StockItem(product, unknownBin, 10)));
            assertEquals(0, db.queryInt("SELECT COUNT(*) FROM stockItems"));
        }

        @Test
        @Disabled("KAN-91")
        @DisplayName("sales order → unknown customer")
        void salesOrderWithUnknownCustomer() throws Exception {
            SalesOrder order = new SalesOrder(unknownCustomer, List.of(new SalesOrderLineItem(product, 1)));

            assertThrows(SQLException.class, () -> salesOrderDao.insert(order));
            assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrders"));
        }

        @Test
        @Disabled("KAN-91")
        @DisplayName("purchase order → unknown supplier")
        void purchaseOrderWithUnknownSupplier() throws Exception {
            PurchaseOrder order = new PurchaseOrder(unknownSupplier, List.of(new PurchaseOrderLineItem(product, 1)));

            assertThrows(SQLException.class, () -> purchaseOrderDao.insert(order));
            assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrders"));
        }

        @Test
        @Disabled("KAN-91")
        @DisplayName("sales order line item → unknown product")
        void salesOrderLineWithUnknownProduct() throws Exception {
            SalesOrder order = new SalesOrder(customer, List.of(new SalesOrderLineItem(unknownProduct, 1)));

            assertThrows(SQLException.class, () -> salesOrderDao.insert(order));
            assertEquals(0, db.queryInt("SELECT COUNT(*) FROM salesOrderLineItems"));
        }

        @Test
        @Disabled("KAN-91")
        @DisplayName("purchase order line item → unknown product")
        void purchaseOrderLineWithUnknownProduct() throws Exception {
            PurchaseOrder order = new PurchaseOrder(supplier, List.of(new PurchaseOrderLineItem(unknownProduct, 1)));

            assertThrows(SQLException.class, () -> purchaseOrderDao.insert(order));
            assertEquals(0, db.queryInt("SELECT COUNT(*) FROM purchaseOrderLineItems"));
        }
    }

    @Nested
    @DisplayName("Delete of a parent that is still referenced is rejected")
    class DeleteReferencedParent {

        @Test
        @Disabled("KAN-91")
        @DisplayName("supplier with products")
        void supplierWithProducts() throws Exception {
            assertThrows(SQLException.class, () -> supplierDao.delete(supplier.getSupplierId()));
            assertEquals(1, db.queryInt("SELECT COUNT(*) FROM suppliers WHERE supplierId = 1"));
        }

        @Test
        @Disabled("KAN-91")
        @DisplayName("product with stock")
        void productWithStock() throws Exception {
            stockItemDao.insert(new StockItem(product, bin, 10));

            assertThrows(SQLException.class, () -> productDao.delete(product.getProductId()));
            assertEquals(1, db.queryInt("SELECT COUNT(*) FROM products WHERE productId = 1"));
        }

        @Test
        @Disabled("KAN-91")
        @DisplayName("bin with stock")
        void binWithStock() throws Exception {
            stockItemDao.insert(new StockItem(product, bin, 10));

            assertThrows(SQLException.class, () -> binDao.delete(bin.getBinId()));
            assertEquals(1, db.queryInt("SELECT COUNT(*) FROM bins WHERE binId = 1"));
        }

        @Test
        @Disabled("KAN-91")
        @DisplayName("customer with sales orders")
        void customerWithOrders() throws Exception {
            salesOrderDao.insert(new SalesOrder(customer, List.of(new SalesOrderLineItem(product, 1))));

            assertThrows(SQLException.class, () -> customerDao.delete(customer.getCustomerId()));
            assertEquals(1, db.queryInt("SELECT COUNT(*) FROM customers WHERE customerId = 1"));
        }
    }
}
