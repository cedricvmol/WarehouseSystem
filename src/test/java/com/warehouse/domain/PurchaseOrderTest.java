package com.warehouse.domain;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Epic("Orders")
@Feature("Purchase order")
@DisplayName("PurchaseOrder")
class PurchaseOrderTest {

    private final Supplier supplier = new Supplier(1, "Parts Inc", "parts@example.com");
    private final Product product = new Product(1, "Widget", 5, supplier);

    private PurchaseOrder orderWithStatus(OrderStatus status) {
        return new PurchaseOrder(1, supplier, LocalDateTime.now(), status,
                List.of(new PurchaseOrderLineItem(product, 10)));
    }

    @Nested
    @DisplayName("Create")
    class Create {

        @Test
        @Story("Create purchase order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("New order starts with status OPEN")
        void newOrderIsOpen() {
            PurchaseOrder order = new PurchaseOrder(supplier, List.of(new PurchaseOrderLineItem(product, 10)));

            assertEquals(OrderStatus.OPEN, order.getOrderStatus());
        }

        @Test
        @Story("Create purchase order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Order without line items (empty list) is rejected")
        void emptyLineItemsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PurchaseOrder(supplier, new ArrayList<>()));
        }

        @Test
        @Story("Create purchase order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Order without line items (null) is rejected")
        void nullLineItemsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PurchaseOrder(supplier, null));
        }
    }

    @Nested
    @DisplayName("Cancel")
    class Cancel {

        @Test
        @Story("Cancel purchase order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("OPEN order can be cancelled")
        void openOrderCanBeCancelled() {
            PurchaseOrder order = orderWithStatus(OrderStatus.OPEN);

            order.cancel();

            assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        }

        @ParameterizedTest(name = "{0} order cannot be cancelled")
        @EnumSource(value = OrderStatus.class, names = {"CANCELLED", "COMPLETED"})
        @Story("Cancel purchase order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Non-OPEN order cannot be cancelled")
        void nonOpenOrderCannotBeCancelled(OrderStatus status) {
            PurchaseOrder order = orderWithStatus(status);

            assertThrows(IllegalStateException.class, order::cancel);
            assertEquals(status, order.getOrderStatus(), "status must not change after a rejected cancel");
        }
    }
}
