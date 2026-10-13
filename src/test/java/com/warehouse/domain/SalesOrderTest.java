package com.warehouse.domain;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@Epic("Orders")
@Feature("Sales order")
@DisplayName("SalesOrder")
class SalesOrderTest {

    private final Customer customer = new Customer(1, "Acme", "acme@example.com");
    private final Product product = new Product(1, "Widget", 5, new Supplier(1, "Parts Inc", "parts@example.com"));

    private SalesOrder orderWithStatus(OrderStatus status) {
        return new SalesOrder(1, customer, LocalDateTime.now(), status,
                List.of(new SalesOrderLineItem(product, 3)));
    }

    @Nested
    @DisplayName("Create")
    class Create {

        @Test
        @Story("Create sales order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("New order starts with status OPEN")
        void newOrderIsOpen() {
            SalesOrder order = new SalesOrder(customer, List.of(new SalesOrderLineItem(product, 3)));

            assertEquals(OrderStatus.OPEN, order.getOrderStatus());
        }

        @Test
        @Story("Create sales order")
        @DisplayName("New order keeps customer and line items")
        void newOrderKeepsCustomerAndLineItems() {
            SalesOrderLineItem line = new SalesOrderLineItem(product, 3);

            SalesOrder order = new SalesOrder(customer, List.of(line));

            assertAll(
                    () -> assertSame(customer, order.getCustomer()),
                    () -> assertEquals(List.of(line), order.getSalesOrderLineItems())
            );
        }

        @Test
        @Story("Create sales order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Order without line items (empty list) is rejected")
        void emptyLineItemsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SalesOrder(customer, new ArrayList<>()));
        }

        @Test
        @Story("Create sales order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Order without line items (null) is rejected")
        void nullLineItemsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new SalesOrder(customer, null));
        }
    }

    @Nested
    @DisplayName("Cancel")
    class Cancel {

        @Test
        @Story("Cancel sales order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("OPEN order can be cancelled")
        void openOrderCanBeCancelled() {
            SalesOrder order = orderWithStatus(OrderStatus.OPEN);

            order.cancel();

            assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        }

        @ParameterizedTest(name = "{0} order cannot be cancelled")
        @EnumSource(value = OrderStatus.class, names = {"CANCELLED", "COMPLETED"})
        @Story("Cancel sales order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Non-OPEN order cannot be cancelled")
        void nonOpenOrderCannotBeCancelled(OrderStatus status) {
            SalesOrder order = orderWithStatus(status);

            assertThrows(IllegalStateException.class, order::cancel);
            assertEquals(status, order.getOrderStatus(), "status must not change after a rejected cancel");
        }
    }

    // ---- Specification tests: written before the fix, enable when working on the ticket ----

    @Nested
    @Issue("KAN-88")
    @DisplayName("KAN-88 - line items cannot be changed from outside")
    class LineItemsEncapsulation {

        @Test
        @Disabled("KAN-88 - not implemented yet")
        @Story("Create sales order")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("Clearing the returned line item list does not empty the order")
        void returnedListCannotEmptyTheOrder() {
            SalesOrder order = new SalesOrder(customer, new ArrayList<>(List.of(new SalesOrderLineItem(product, 3))));

            try {
                order.getSalesOrderLineItems().clear();
            } catch (UnsupportedOperationException expected) {
                // an unmodifiable list is a valid way to protect the order
            }

            assertEquals(1, order.getSalesOrderLineItems().size());
        }

        @Test
        @Disabled("KAN-88 - not implemented yet")
        @Story("Create sales order")
        @DisplayName("Changing the list passed to the constructor afterwards does not change the order")
        void constructorListIsNotShared() {
            List<SalesOrderLineItem> lines = new ArrayList<>(List.of(new SalesOrderLineItem(product, 3)));
            SalesOrder order = new SalesOrder(customer, lines);

            lines.clear();

            assertEquals(1, order.getSalesOrderLineItems().size());
        }
    }
}
