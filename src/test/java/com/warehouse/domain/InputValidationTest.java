package com.warehouse.domain;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Specification tests for KAN-87 "Reject invalid input for master data and orders".
 * One nested class per acceptance criterion. Rejection tests are @Disabled until
 * the criterion is implemented; the "allowed" boundary tests already run and guard
 * against rejecting too much.
 */
@Tag("unit")
@Epic("Validation")
@Feature("Input validation")
@Story("Reject invalid input")
@Issue("KAN-87")
@DisplayName("KAN-87 - Input validation")
class InputValidationTest {

    private final Supplier supplier = new Supplier(1, "Parts Inc", "parts@example.com");
    private final Customer customer = new Customer(1, "Acme", "acme@example.com");
    private final Product product = new Product(1, "Widget", 5, supplier);
    private final Bin bin = new Bin(1, "A1");

    @Nested
    @DisplayName("AC1 - line item quantity must be above 0")
    class LineItemQuantity {

        @ParameterizedTest(name = "quantity {0} is rejected")
        @ValueSource(ints = {0, -1})
        @Disabled("KAN-87 AC1 - not implemented yet")
        @DisplayName("Purchase order line item with quantity <= 0 is rejected")
        void purchaseLineRejected(int quantity) {
            assertThrows(IllegalArgumentException.class, () -> new PurchaseOrderLineItem(product, quantity));
        }

        @ParameterizedTest(name = "quantity {0} is rejected")
        @ValueSource(ints = {0, -1})
        @Disabled("KAN-87 AC1 - not implemented yet")
        @DisplayName("Sales order line item with quantity <= 0 is rejected")
        void salesLineRejected(int quantity) {
            assertThrows(IllegalArgumentException.class, () -> new SalesOrderLineItem(product, quantity));
        }

        @Test
        @DisplayName("Quantity 1 is allowed (boundary)")
        void quantityOneAllowed() {
            assertDoesNotThrow(() -> new PurchaseOrderLineItem(product, 1));
            assertDoesNotThrow(() -> new SalesOrderLineItem(product, 1));
        }
    }

    @Nested
    @DisplayName("AC2 - stock quantity must not be negative")
    class StockQuantity {

        @Test
        @Disabled("KAN-87 AC2 - not implemented yet")
        @DisplayName("Stock quantity -1 is rejected")
        void negativeRejected() {
            assertThrows(IllegalArgumentException.class, () -> new StockItem(product, bin, -1));
        }

        @Test
        @DisplayName("Stock quantity 0 is allowed (boundary)")
        void zeroAllowed() {
            assertDoesNotThrow(() -> new StockItem(product, bin, 0));
        }
    }

    @Nested
    @DisplayName("AC3 - product threshold must not be negative")
    class Threshold {

        @Test
        @Disabled("KAN-87 AC3 - not implemented yet")
        @DisplayName("Threshold -1 is rejected")
        void negativeRejected() {
            assertThrows(IllegalArgumentException.class, () -> new Product(1, "Widget", -1, supplier));
        }

        @Test
        @DisplayName("Threshold 0 is allowed (boundary)")
        void zeroAllowed() {
            assertDoesNotThrow(() -> new Product(1, "Widget", 0, supplier));
        }
    }

    @Nested
    @DisplayName("AC4 - text fields must not be empty, blank or missing")
    class RequiredText {

        @ParameterizedTest(name = "product name [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @Disabled("KAN-87 AC4 - not implemented yet")
        @DisplayName("Product without name is rejected")
        void productName(String name) {
            assertThrows(IllegalArgumentException.class, () -> new Product(1, name, 5, supplier));
        }

        @ParameterizedTest(name = "customer name [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @Disabled("KAN-87 AC4 - not implemented yet")
        @DisplayName("Customer without name is rejected")
        void customerName(String name) {
            assertThrows(IllegalArgumentException.class, () -> new Customer(1, name, "acme@example.com"));
        }

        @ParameterizedTest(name = "customer e-mail [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @Disabled("KAN-87 AC4 - not implemented yet")
        @DisplayName("Customer without e-mail is rejected")
        void customerEmail(String email) {
            assertThrows(IllegalArgumentException.class, () -> new Customer(1, "Acme", email));
        }

        @ParameterizedTest(name = "supplier name [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @Disabled("KAN-87 AC4 - not implemented yet")
        @DisplayName("Supplier without name is rejected")
        void supplierName(String name) {
            assertThrows(IllegalArgumentException.class, () -> new Supplier(1, name, "parts@example.com"));
        }

        @ParameterizedTest(name = "supplier e-mail [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @Disabled("KAN-87 AC4 - not implemented yet")
        @DisplayName("Supplier without e-mail is rejected")
        void supplierEmail(String email) {
            assertThrows(IllegalArgumentException.class, () -> new Supplier(1, "Parts Inc", email));
        }

        @ParameterizedTest(name = "bin location code [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @Disabled("KAN-87 AC4 - not implemented yet")
        @DisplayName("Bin without location code is rejected")
        void binLocationCode(String locationCode) {
            assertThrows(IllegalArgumentException.class, () -> new Bin(1, locationCode));
        }
    }

    @Nested
    @DisplayName("AC5 - e-mail needs an @ with text on both sides")
    class EmailFormat {

        @ParameterizedTest(name = "e-mail \"{0}\" is rejected")
        @ValueSource(strings = {"acme", "@example.com", "acme@"})
        @Disabled("KAN-87 AC5 - not implemented yet")
        @DisplayName("Customer with invalid e-mail is rejected")
        void customerInvalid(String email) {
            assertThrows(IllegalArgumentException.class, () -> new Customer(1, "Acme", email));
        }

        @ParameterizedTest(name = "e-mail \"{0}\" is rejected")
        @ValueSource(strings = {"parts", "@example.com", "parts@"})
        @Disabled("KAN-87 AC5 - not implemented yet")
        @DisplayName("Supplier with invalid e-mail is rejected")
        void supplierInvalid(String email) {
            assertThrows(IllegalArgumentException.class, () -> new Supplier(1, "Parts Inc", email));
        }

        @Test
        @DisplayName("Shortest valid e-mail a@b is allowed (boundary)")
        void shortestValidAllowed() {
            assertDoesNotThrow(() -> new Customer(1, "Acme", "a@b"));
            assertDoesNotThrow(() -> new Supplier(1, "Parts Inc", "a@b"));
        }
    }

    @Nested
    @DisplayName("AC6 - order needs a customer or supplier")
    class OrderParty {

        @Test
        @Disabled("KAN-87 AC6 - not implemented yet")
        @DisplayName("Purchase order without supplier is rejected")
        void purchaseOrderWithoutSupplier() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PurchaseOrder(null, List.of(new PurchaseOrderLineItem(product, 3))));
        }

        @Test
        @Disabled("KAN-87 AC6 - not implemented yet")
        @DisplayName("Sales order without customer is rejected")
        void salesOrderWithoutCustomer() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SalesOrder(null, List.of(new SalesOrderLineItem(product, 3))));
        }
    }

    @Nested
    @DisplayName("AC7 - a product appears at most once per order")
    class DuplicateProduct {

        // A product loaded twice from the database gives two objects with the same id:
        // "same product" means same productId, not same Java object.
        private final Product sameProductOtherInstance = new Product(1, "Widget", 5, supplier);

        @Test
        @Disabled("KAN-87 AC7 - not implemented yet")
        @DisplayName("Purchase order with the same product twice is rejected")
        void purchaseOrderDuplicate() {
            assertThrows(IllegalArgumentException.class, () -> new PurchaseOrder(supplier, List.of(
                    new PurchaseOrderLineItem(product, 3),
                    new PurchaseOrderLineItem(sameProductOtherInstance, 2))));
        }

        @Test
        @Disabled("KAN-87 AC7 - not implemented yet")
        @DisplayName("Sales order with the same product twice is rejected")
        void salesOrderDuplicate() {
            assertThrows(IllegalArgumentException.class, () -> new SalesOrder(customer, List.of(
                    new SalesOrderLineItem(product, 3),
                    new SalesOrderLineItem(sameProductOtherInstance, 2))));
        }

        @Test
        @DisplayName("Order with two different products is allowed")
        void differentProductsAllowed() {
            Product gadget = new Product(2, "Gadget", 5, supplier);
            assertDoesNotThrow(() -> new SalesOrder(customer, List.of(
                    new SalesOrderLineItem(product, 3),
                    new SalesOrderLineItem(gadget, 2))));
        }
    }

    @Nested
    @DisplayName("AC8 - ids must be above 0")
    class Ids {

        @ParameterizedTest(name = "product id {0} is rejected")
        @ValueSource(ints = {0, -1})
        @Disabled("KAN-87 AC8 - not implemented yet")
        @DisplayName("Product with id <= 0 is rejected")
        void productId(int id) {
            assertThrows(IllegalArgumentException.class, () -> new Product(id, "Widget", 5, supplier));
        }

        @ParameterizedTest(name = "customer id {0} is rejected")
        @ValueSource(ints = {0, -1})
        @Disabled("KAN-87 AC8 - not implemented yet")
        @DisplayName("Customer with id <= 0 is rejected")
        void customerId(int id) {
            assertThrows(IllegalArgumentException.class, () -> new Customer(id, "Acme", "acme@example.com"));
        }

        @ParameterizedTest(name = "supplier id {0} is rejected")
        @ValueSource(ints = {0, -1})
        @Disabled("KAN-87 AC8 - not implemented yet")
        @DisplayName("Supplier with id <= 0 is rejected")
        void supplierId(int id) {
            assertThrows(IllegalArgumentException.class, () -> new Supplier(id, "Parts Inc", "parts@example.com"));
        }

        @ParameterizedTest(name = "bin id {0} is rejected")
        @ValueSource(ints = {0, -1})
        @Disabled("KAN-87 AC8 - not implemented yet")
        @DisplayName("Bin with id <= 0 is rejected")
        void binId(int id) {
            assertThrows(IllegalArgumentException.class, () -> new Bin(id, "A1"));
        }

        @Test
        @DisplayName("Id 1 is allowed (boundary)")
        void idOneAllowed() {
            assertAll(
                    () -> assertDoesNotThrow(() -> new Product(1, "Widget", 5, supplier)),
                    () -> assertDoesNotThrow(() -> new Customer(1, "Acme", "acme@example.com")),
                    () -> assertDoesNotThrow(() -> new Supplier(1, "Parts Inc", "parts@example.com")),
                    () -> assertDoesNotThrow(() -> new Bin(1, "A1"))
            );
        }
    }
}
