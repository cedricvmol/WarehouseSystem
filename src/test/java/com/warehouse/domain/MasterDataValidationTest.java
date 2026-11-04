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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Specification tests for KAN-102 "Reject invalid master data input" (split off from
 * KAN-87; criterion IDs D3, D4, D5, D9 kept from the original ticket).
 * One nested class per criterion. Rejection tests are @Disabled until the criterion is
 * implemented; the "allowed" boundary tests already run and guard against rejecting
 * too much.
 */
@Tag("unit")
@Epic("Validation")
@Feature("Input validation")
@Story("Reject invalid master data input")
@Issue("KAN-102")
@DisplayName("KAN-102 - Master data validation")
class MasterDataValidationTest {

    private final Supplier supplier = new Supplier(1, "Parts Inc", "parts@example.com");

    @Nested
    @DisplayName("D3 - product threshold must not be negative")
    class Threshold {

        @Test
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
    @DisplayName("D4 - text fields must not be empty, blank or missing")
    class RequiredText {

        @ParameterizedTest(name = "product name [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Product without name is rejected")
        void productName(String name) {
            assertThrows(IllegalArgumentException.class, () -> new Product(1, name, 5, supplier));
        }

        @ParameterizedTest(name = "customer name [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Customer without name is rejected")
        void customerName(String name) {
            assertThrows(IllegalArgumentException.class, () -> new Customer(1, name, "acme@example.com"));
        }

        @ParameterizedTest(name = "customer e-mail [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Customer without e-mail is rejected")
        void customerEmail(String email) {
            assertThrows(IllegalArgumentException.class, () -> new Customer(1, "Acme", email));
        }

        @ParameterizedTest(name = "supplier name [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Supplier without name is rejected")
        void supplierName(String name) {
            assertThrows(IllegalArgumentException.class, () -> new Supplier(1, name, "parts@example.com"));
        }

        @ParameterizedTest(name = "supplier e-mail [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Supplier without e-mail is rejected")
        void supplierEmail(String email) {
            assertThrows(IllegalArgumentException.class, () -> new Supplier(1, "Parts Inc", email));
        }

        @ParameterizedTest(name = "bin location code [{0}] is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Bin without location code is rejected")
        void binLocationCode(String locationCode) {
            assertThrows(IllegalArgumentException.class, () -> new Bin(1, locationCode));
        }
    }

    @Nested
    @DisplayName("D5 - e-mail needs an @ with text on both sides")
    class EmailFormat {

        @ParameterizedTest(name = "e-mail \"{0}\" is rejected")
        @ValueSource(strings = {"acme", "@example.com", "acme@"})
        @Disabled("KAN-102 D5 - not implemented yet")
        @DisplayName("Customer with invalid e-mail is rejected")
        void customerInvalid(String email) {
            assertThrows(IllegalArgumentException.class, () -> new Customer(1, "Acme", email));
        }

        @ParameterizedTest(name = "e-mail \"{0}\" is rejected")
        @ValueSource(strings = {"parts", "@example.com", "parts@"})
        @Disabled("KAN-102 D5 - not implemented yet")
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
    @DisplayName("D9 - ids must be above 0")
    class Ids {

        @ParameterizedTest(name = "product id {0} is rejected")
        @ValueSource(ints = {0, -1})
        @DisplayName("Product with id <= 0 is rejected")
        void productId(int id) {
            assertThrows(IllegalArgumentException.class, () -> new Product(id, "Widget", 5, supplier));
        }

        @ParameterizedTest(name = "customer id {0} is rejected")
        @ValueSource(ints = {0, -1})
        @DisplayName("Customer with id <= 0 is rejected")
        void customerId(int id) {
            assertThrows(IllegalArgumentException.class, () -> new Customer(id, "Acme", "acme@example.com"));
        }

        @ParameterizedTest(name = "supplier id {0} is rejected")
        @ValueSource(ints = {0, -1})
        @DisplayName("Supplier with id <= 0 is rejected")
        void supplierId(int id) {
            assertThrows(IllegalArgumentException.class, () -> new Supplier(id, "Parts Inc", "parts@example.com"));
        }

        @ParameterizedTest(name = "bin id {0} is rejected")
        @ValueSource(ints = {0, -1})
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
