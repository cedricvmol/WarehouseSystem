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
}
