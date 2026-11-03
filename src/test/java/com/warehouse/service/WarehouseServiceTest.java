package com.warehouse.service;

import com.warehouse.domain.*;
import com.warehouse.storage.*;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WarehouseService} with mocked DAOs: no database involved.
 * Master data DAOs (product, bin, supplier, customer) return Optional.empty()
 * when nothing is found (KAN-103); the order DAOs still throw
 * IllegalArgumentException. The mocks reproduce that to test how the service reacts.
 */
@Tag("unit")
@Epic("Warehouse service")
@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseService")
class WarehouseServiceTest {

    @Mock private ProductDao productDao;
    @Mock private BinDao binDao;
    @Mock private CustomerDao customerDao;
    @Mock private SupplierDao supplierDao;
    @Mock private StockItemDao stockItemDao;
    @Mock private PurchaseOrderDao purchaseOrderDao;
    @Mock private SalesOrderDao salesOrderDao;

    private WarehouseService service;

    private final Supplier supplier = new Supplier(7, "Parts Inc", "parts@example.com");
    private final Customer customer = new Customer(8, "Acme", "acme@example.com");
    private final Product widget = new Product(1, "Widget", 5, supplier);
    private final Product gadget = new Product(2, "Gadget", 10, supplier);
    private final Bin binA1 = new Bin(1, "A1");

    @BeforeEach
    void setUp() {
        service = new WarehouseService(productDao, binDao, customerDao, supplierDao,
                stockItemDao, purchaseOrderDao, salesOrderDao);
    }

    private static IllegalArgumentException notFound(String what) {
        return new IllegalArgumentException("No " + what + " found.");
    }

    @Nested
    @Feature("Master data")
    @DisplayName("Master data")
    class MasterData {

        @Test
        @Story("Add product")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Add product with existing supplier stores it with that supplier")
        void addProductWithExistingSupplier() throws Exception {
            when(supplierDao.findById(7)).thenReturn(Optional.of(supplier));

            service.addProduct(1, "Widget", 5, 7);

            ArgumentCaptor<Product> stored = ArgumentCaptor.forClass(Product.class);
            verify(productDao).insert(stored.capture());
            assertAll(
                    () -> assertEquals(1, stored.getValue().getProductId()),
                    () -> assertEquals("Widget", stored.getValue().getName()),
                    () -> assertEquals(5, stored.getValue().getThreshold()),
                    () -> assertSame(supplier, stored.getValue().getSupplier())
            );
        }

        @Test
        @Story("Add product")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Add product with unknown supplier is rejected and nothing is stored")
        void addProductWithUnknownSupplier() throws Exception {
            when(supplierDao.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.addProduct(1, "Widget", 5, 99));
            verify(productDao, never()).insert(any());
        }

        @Test
        @Story("Add bin")
        @DisplayName("Add bin stores id and location code")
        void addBin() throws Exception {
            service.addBin(3, "B2");

            ArgumentCaptor<Bin> stored = ArgumentCaptor.forClass(Bin.class);
            verify(binDao).insert(stored.capture());
            assertAll(
                    () -> assertEquals(3, stored.getValue().getBinId()),
                    () -> assertEquals("B2", stored.getValue().getLocationCode())
            );
        }

        @Test
        @Story("Add customer")
        @DisplayName("Add customer stores id, name and e-mail")
        void addCustomer() throws Exception {
            service.addCustomer(8, "Acme", "acme@example.com");

            ArgumentCaptor<Customer> stored = ArgumentCaptor.forClass(Customer.class);
            verify(customerDao).insert(stored.capture());
            assertAll(
                    () -> assertEquals(8, stored.getValue().getCustomerId()),
                    () -> assertEquals("Acme", stored.getValue().getName()),
                    () -> assertEquals("acme@example.com", stored.getValue().getEmail())
            );
        }

        @Test
        @Story("Add supplier")
        @DisplayName("Add supplier stores id, name and e-mail")
        void addSupplier() throws Exception {
            service.addSupplier(7, "Parts Inc", "parts@example.com");

            ArgumentCaptor<Supplier> stored = ArgumentCaptor.forClass(Supplier.class);
            verify(supplierDao).insert(stored.capture());
            assertAll(
                    () -> assertEquals(7, stored.getValue().getSupplierId()),
                    () -> assertEquals("Parts Inc", stored.getValue().getName()),
                    () -> assertEquals("parts@example.com", stored.getValue().getEmail())
            );
        }
    }

    @Nested
    @Feature("Master data")
    @Issue("KAN-103")
    @DisplayName("KAN-103 - duplicate ID is rejected")
    class DuplicateId {

        @Test
        @Story("Add product")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Add product with existing ID is rejected with a clear message, nothing stored")
        void addProductWithExistingId() throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.addProduct(1, "Other", 3, 7));
            assertEquals("A product with id 1 already exists.", e.getMessage());
            verify(productDao, never()).insert(any());
        }

        @Test
        @Story("Add supplier")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Add supplier with existing ID is rejected with a clear message, nothing stored")
        void addSupplierWithExistingId() throws Exception {
            when(supplierDao.findById(7)).thenReturn(Optional.of(supplier));

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.addSupplier(7, "Other", "other@example.com"));
            assertEquals("A supplier with id 7 already exists.", e.getMessage());
            verify(supplierDao, never()).insert(any());
        }

        @Test
        @Story("Add customer")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Add customer with existing ID is rejected with a clear message, nothing stored")
        void addCustomerWithExistingId() throws Exception {
            when(customerDao.findById(8)).thenReturn(Optional.of(customer));

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.addCustomer(8, "Other", "other@example.com"));
            assertEquals("A customer with id 8 already exists.", e.getMessage());
            verify(customerDao, never()).insert(any());
        }

        @Test
        @Story("Add bin")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Add bin with existing ID is rejected with a clear message, nothing stored")
        void addBinWithExistingId() throws Exception {
            when(binDao.findById(1)).thenReturn(Optional.of(binA1));

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.addBin(1, "A-02-02"));
            assertEquals("A bin with id 1 already exists.", e.getMessage());
            verify(binDao, never()).insert(any());
        }
    }

    @Nested
    @Feature("Master data")
    @DisplayName("List master data (KAN-95)")
    class ListMasterData {

        @Test
        @Story("List products")
        @DisplayName("List products returns every product from the DAO")
        void listProducts() throws Exception {
            when(productDao.findAll()).thenReturn(List.of(widget, gadget));

            assertEquals(List.of(widget, gadget), service.giveListAllProducts());
        }

        @Test
        @Story("List suppliers")
        @DisplayName("List suppliers returns every supplier from the DAO")
        void listSuppliers() throws Exception {
            when(supplierDao.findAll()).thenReturn(List.of(supplier));

            assertEquals(List.of(supplier), service.giveListAllSuppliers());
        }

        @Test
        @Story("List customers")
        @DisplayName("List customers returns every customer from the DAO")
        void listCustomers() throws Exception {
            when(customerDao.findAll()).thenReturn(List.of(customer));

            assertEquals(List.of(customer), service.giveListAllCustomers());
        }

        @Test
        @Story("List bins")
        @DisplayName("List bins returns every bin from the DAO")
        void listBins() throws Exception {
            when(binDao.findAll()).thenReturn(List.of(binA1));

            assertEquals(List.of(binA1), service.giveListAllBins());
        }

        @Test
        @Story("List products")
        @DisplayName("Empty table gives an empty list, not null")
        void emptyListIsNotNull() throws Exception {
            when(productDao.findAll()).thenReturn(List.of());

            assertEquals(List.of(), service.giveListAllProducts());
        }
    }

    @Nested
    @Feature("Stock")
    @DisplayName("Stock")
    class Stock {

        @Test
        @Story("Add stock")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Add stock for existing product and bin stores it")
        void addStockItem() throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));
            when(binDao.findById(1)).thenReturn(Optional.of(binA1));

            service.addStockItem(1, 1, 20);

            ArgumentCaptor<StockItem> stored = ArgumentCaptor.forClass(StockItem.class);
            verify(stockItemDao).insert(stored.capture());
            assertAll(
                    () -> assertSame(widget, stored.getValue().getProduct()),
                    () -> assertSame(binA1, stored.getValue().getBin()),
                    () -> assertEquals(20, stored.getValue().getQuantity())
            );
        }

        @Test
        @Story("Add stock")
        @DisplayName("Add stock for unknown product is rejected and nothing is stored")
        void addStockItemUnknownProduct() throws Exception {
            when(productDao.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.addStockItem(99, 1, 20));
            verify(stockItemDao, never()).insert(any());
        }

        @Test
        @Story("Add stock")
        @DisplayName("Add stock for unknown bin is rejected and nothing is stored")
        void addStockItemUnknownBin() throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));
            when(binDao.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.addStockItem(1, 99, 20));
            verify(stockItemDao, never()).insert(any());
        }

        @Test
        @Story("Check stock level")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Stock level is the sum over all bins")
        void stockLevelSumsAllBins() throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));
            when(stockItemDao.findByProduct(1)).thenReturn(List.of(
                    new StockItem(widget, new Bin(1, "A1"), 3),
                    new StockItem(widget, new Bin(2, "A2"), 4),
                    new StockItem(widget, new Bin(3, "A3"), 5)));

            assertEquals(12, service.checkStockLevel(1));
        }

        @Test
        @Story("Check stock level")
        @DisplayName("Product without stock has stock level 0")
        void stockLevelWithoutStockIsZero() throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));
            when(stockItemDao.findByProduct(1)).thenReturn(List.of());

            assertEquals(0, service.checkStockLevel(1));
        }

        @Test
        @Story("Check stock level")
        @DisplayName("Stock level of unknown product is rejected before stock is queried")
        void stockLevelUnknownProduct() throws Exception {
            when(productDao.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.checkStockLevel(99));
            verify(stockItemDao, never()).findByProduct(anyInt());
        }
    }

    @Nested
    @Feature("Purchase order")
    @DisplayName("Purchase orders")
    class PurchaseOrders {

        private PurchaseOrder orderWithStatus(OrderStatus status) {
            return new PurchaseOrder(5, supplier, LocalDateTime.now(), status,
                    List.of(new PurchaseOrderLineItem(widget, 3)));
        }

        @Test
        @Story("Create purchase order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Create stores an OPEN order with supplier and line items, returns the generated id")
        void createPurchaseOrder() throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));
            when(productDao.findById(2)).thenReturn(Optional.of(gadget));
            when(supplierDao.findById(7)).thenReturn(Optional.of(supplier));
            when(purchaseOrderDao.insert(any())).thenReturn(42);
            Map<Integer, Integer> lines = new LinkedHashMap<>();
            lines.put(1, 3);
            lines.put(2, 4);

            int id = service.createPurchaseOrder(7, lines);

            ArgumentCaptor<PurchaseOrder> stored = ArgumentCaptor.forClass(PurchaseOrder.class);
            verify(purchaseOrderDao).insert(stored.capture());
            Map<Integer, Integer> storedLines = stored.getValue().getPurchaseOrderLineItems().stream()
                    .collect(Collectors.toMap(l -> l.getProduct().getProductId(), PurchaseOrderLineItem::getQuantity));
            assertAll(
                    () -> assertEquals(42, id),
                    () -> assertEquals(OrderStatus.OPEN, stored.getValue().getOrderStatus()),
                    () -> assertSame(supplier, stored.getValue().getSupplier()),
                    () -> assertEquals(Map.of(1, 3, 2, 4), storedLines)
            );
        }

        @Test
        @Story("Create purchase order")
        @DisplayName("Create with unknown product is rejected and nothing is stored")
        void createWithUnknownProduct() throws Exception {
            when(productDao.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.createPurchaseOrder(7, Map.of(99, 3)));
            verify(purchaseOrderDao, never()).insert(any());
        }

        @Test
        @Story("Create purchase order")
        @DisplayName("Create with unknown supplier is rejected and nothing is stored")
        void createWithUnknownSupplier() throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));
            when(supplierDao.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.createPurchaseOrder(99, Map.of(1, 3)));
            verify(purchaseOrderDao, never()).insert(any());
        }

        @Test
        @Story("Create purchase order")
        @DisplayName("Create without line items is rejected and nothing is stored")
        void createWithoutLineItems() throws Exception {
            when(supplierDao.findById(7)).thenReturn(Optional.of(supplier));

            assertThrows(IllegalArgumentException.class, () -> service.createPurchaseOrder(7, new HashMap<>()));
            verify(purchaseOrderDao, never()).insert(any());
        }

        @Test
        @Story("Cancel purchase order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Cancel OPEN order stores it as CANCELLED")
        void cancelOpenOrder() throws Exception {
            PurchaseOrder order = orderWithStatus(OrderStatus.OPEN);
            when(purchaseOrderDao.findById(5)).thenReturn(order);

            service.cancelPurchaseOrder(5);

            verify(purchaseOrderDao).update(order);
            assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        }

        @Test
        @Story("Cancel purchase order")
        @DisplayName("Cancel already cancelled order is rejected and nothing is updated")
        void cancelCancelledOrder() throws Exception {
            when(purchaseOrderDao.findById(5)).thenReturn(orderWithStatus(OrderStatus.CANCELLED));

            assertThrows(IllegalStateException.class, () -> service.cancelPurchaseOrder(5));
            verify(purchaseOrderDao, never()).update(any());
        }

        @Test
        @Story("Cancel purchase order")
        @DisplayName("Cancel unknown order is rejected and nothing is updated")
        void cancelUnknownOrder() throws Exception {
            when(purchaseOrderDao.findById(99)).thenThrow(notFound("purchase order"));

            assertThrows(IllegalArgumentException.class, () -> service.cancelPurchaseOrder(99));
            verify(purchaseOrderDao, never()).update(any());
        }
    }

    @Nested
    @Feature("Sales order")
    @DisplayName("Sales orders")
    class SalesOrders {

        private SalesOrder orderWithStatus(OrderStatus status) {
            return new SalesOrder(5, customer, LocalDateTime.now(), status,
                    List.of(new SalesOrderLineItem(widget, 3)));
        }

        @Test
        @Story("Create sales order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Create stores an OPEN order with customer and line items, returns the generated id")
        void createSalesOrder() throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));
            when(productDao.findById(2)).thenReturn(Optional.of(gadget));
            when(customerDao.findById(8)).thenReturn(Optional.of(customer));
            when(salesOrderDao.insert(any())).thenReturn(43);
            Map<Integer, Integer> lines = new LinkedHashMap<>();
            lines.put(1, 3);
            lines.put(2, 4);

            int id = service.createSalesOrder(8, lines);

            ArgumentCaptor<SalesOrder> stored = ArgumentCaptor.forClass(SalesOrder.class);
            verify(salesOrderDao).insert(stored.capture());
            Map<Integer, Integer> storedLines = stored.getValue().getSalesOrderLineItems().stream()
                    .collect(Collectors.toMap(l -> l.getProduct().getProductId(), SalesOrderLineItem::getQuantity));
            assertAll(
                    () -> assertEquals(43, id),
                    () -> assertEquals(OrderStatus.OPEN, stored.getValue().getOrderStatus()),
                    () -> assertSame(customer, stored.getValue().getCustomer()),
                    () -> assertEquals(Map.of(1, 3, 2, 4), storedLines)
            );
        }

        @Test
        @Story("Create sales order")
        @DisplayName("Create with unknown product is rejected and nothing is stored")
        void createWithUnknownProduct() throws Exception {
            when(productDao.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.createSalesOrder(8, Map.of(99, 3)));
            verify(salesOrderDao, never()).insert(any());
        }

        @Test
        @Story("Create sales order")
        @DisplayName("Create with unknown customer is rejected and nothing is stored")
        void createWithUnknownCustomer() throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));
            when(customerDao.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.createSalesOrder(99, Map.of(1, 3)));
            verify(salesOrderDao, never()).insert(any());
        }

        @Test
        @Story("Create sales order")
        @DisplayName("Create without line items is rejected and nothing is stored")
        void createWithoutLineItems() throws Exception {
            when(customerDao.findById(8)).thenReturn(Optional.of(customer));

            assertThrows(IllegalArgumentException.class, () -> service.createSalesOrder(8, new HashMap<>()));
            verify(salesOrderDao, never()).insert(any());
        }

        @Test
        @Story("Cancel sales order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Cancel OPEN order stores it as CANCELLED")
        void cancelOpenOrder() throws Exception {
            SalesOrder order = orderWithStatus(OrderStatus.OPEN);
            when(salesOrderDao.findById(5)).thenReturn(order);

            service.cancelSalesOrder(5);

            verify(salesOrderDao).update(order);
            assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        }

        @Test
        @Story("Cancel sales order")
        @DisplayName("Cancel already cancelled order is rejected and nothing is updated")
        void cancelCancelledOrder() throws Exception {
            when(salesOrderDao.findById(5)).thenReturn(orderWithStatus(OrderStatus.CANCELLED));

            assertThrows(IllegalStateException.class, () -> service.cancelSalesOrder(5));
            verify(salesOrderDao, never()).update(any());
        }

        @Test
        @Story("Cancel sales order")
        @DisplayName("Cancel unknown order is rejected and nothing is updated")
        void cancelUnknownOrder() throws Exception {
            when(salesOrderDao.findById(99)).thenThrow(notFound("sales order"));

            assertThrows(IllegalArgumentException.class, () -> service.cancelSalesOrder(99));
            verify(salesOrderDao, never()).update(any());
        }
    }

    // ---- Specification tests: written before the fix, enable when working on the ticket ----

    @Nested
    @Feature("Purchase order")
    @Issue("KAN-89")
    @DisplayName("KAN-89 - null line items")
    class NullLineItems {

        @Test
        @Story("Create purchase order")
        @DisplayName("Create purchase order with null line items gives IllegalArgumentException, nothing stored")
        void createPurchaseOrderWithNullLineItems() throws Exception {
            assertThrows(IllegalArgumentException.class, () -> service.createPurchaseOrder(7, null));
            verify(purchaseOrderDao, never()).insert(any());
        }

        @Test
        @Story("Create sales order")
        @DisplayName("Create sales order with null line items gives IllegalArgumentException, nothing stored")
        void createSalesOrderWithNullLineItems() throws Exception {
            assertThrows(IllegalArgumentException.class, () -> service.createSalesOrder(8, null));
            verify(salesOrderDao, never()).insert(any());
        }
    }

    @Nested
    @Feature("Input validation")
    @Issue("KAN-87")
    @DisplayName("KAN-87 AC1 - line item quantity must be above 0")
    class LineItemQuantity {

        @ParameterizedTest(name = "quantity {0} is rejected")
        @ValueSource(ints = {0, -1})
        @Disabled("KAN-87 AC1 - not implemented yet")
        @Story("Reject invalid input")
        @DisplayName("Purchase order with quantity <= 0 is rejected, nothing stored")
        void purchaseOrderQuantityRejected(int quantity) throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));

            assertThrows(IllegalArgumentException.class, () -> service.createPurchaseOrder(7, Map.of(1, quantity)));
            verify(purchaseOrderDao, never()).insert(any());
        }

        @ParameterizedTest(name = "quantity {0} is rejected")
        @ValueSource(ints = {0, -1})
        @Disabled("KAN-87 AC1 - not implemented yet")
        @Story("Reject invalid input")
        @DisplayName("Sales order with quantity <= 0 is rejected, nothing stored")
        void salesOrderQuantityRejected(int quantity) throws Exception {
            when(productDao.findById(1)).thenReturn(Optional.of(widget));

            assertThrows(IllegalArgumentException.class, () -> service.createSalesOrder(8, Map.of(1, quantity)));
            verify(salesOrderDao, never()).insert(any());
        }
    }
}
