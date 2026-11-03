package com.warehouse.domain;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for KAN-87 "Reject invalid order and stock input" (criteria D1, D2, D6).
 * One nested class per criterion, each with the rejected values and the allowed
 * boundary. Master data validation (D3, D4, D5, D9) moved to KAN-102, see
 * MasterDataValidationTest.
 */
@Tag("unit")
@Epic("Validation")
@Feature("Input validation")
@Story("Reject invalid order and stock input")
@Issue("KAN-87")
@DisplayName("KAN-87 - Order and stock validation")
class InputValidationTest {

    private final Supplier supplier = new Supplier(1, "Parts Inc", "parts@example.com");
    private final Product product = new Product(1, "Widget", 5, supplier);
    private final Bin bin = new Bin(1, "A1");

    @Nested
    @DisplayName("D1 - line item quantity must be above 0")
    class LineItemQuantity {

        @ParameterizedTest(name = "quantity {0} is rejected")
        @ValueSource(ints = {0, -1})
        @DisplayName("Purchase order line item with quantity <= 0 is rejected")
        void purchaseLineRejected(int quantity) {
            assertThrows(IllegalArgumentException.class, () -> new PurchaseOrderLineItem(product, quantity));
        }

        @ParameterizedTest(name = "quantity {0} is rejected")
        @ValueSource(ints = {0, -1})
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
    @DisplayName("D2 - stock quantity must not be negative")
    class StockQuantity {

        @Test
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
    @DisplayName("D6 - order needs a customer or supplier")
    class OrderParty {

        @Test
        @DisplayName("Purchase order without supplier is rejected")
        void purchaseOrderWithoutSupplier() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PurchaseOrder(null, List.of(new PurchaseOrderLineItem(product, 3))));
        }

        @Test
        @DisplayName("Sales order without customer is rejected")
        void salesOrderWithoutCustomer() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SalesOrder(null, List.of(new SalesOrderLineItem(product, 3))));
        }
    }

    // D7 ("same product twice in one order is rejected") was removed from KAN-87 on
    // 2026-11-04: the UI sums quantities per productId, so duplicate lines never reach
    // the domain. That summing is pinned in WarehouseUIPurchaseOrderTest and
    // WarehouseUISalesOrderTest (sameProductTwiceIsSummed / sameProductNotConsecutive).
}
