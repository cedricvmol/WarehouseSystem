package com.warehouse.ui;

import com.warehouse.service.WarehouseService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The "Cancel purchase order" (menu [8]) and "Cancel sales order" (menu [10])
 * CLI flows with a mocked service. All tests go through run(), so the menu
 * routing is covered too. The status rules themselves are in
 * PurchaseOrderTest / SalesOrderTest and WarehouseServiceTest.
 */
@Tag("unit")
@Epic("CLI")
@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseUI - cancel order")
class WarehouseUICancelOrderTest {

    @Mock private WarehouseService service;

    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @BeforeEach
    void captureOutput() {
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreConsole() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    /** The UI builds its Scanner in the constructor, so the input must be set first. */
    private WarehouseUI uiWithInput(String... lines) {
        String input = String.join("\n", lines) + "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        return new WarehouseUI(service);
    }

    private String output() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Nested
    @Feature("Purchase order")
    @Story("Cancel purchase order")
    @DisplayName("Cancel purchase order [8]")
    class PurchaseOrder {

        @Test
        @DisplayName("Open order: id goes to the service, confirmation is shown")
        void cancelOpen() throws Exception {
            WarehouseUI ui = uiWithInput("8", "5", "0");

            ui.run();

            verify(service).cancelPurchaseOrder(5);
            assertTrue(output().contains("Purchase order #5 cancelled."), output());
        }

        @Test
        @DisplayName("Order not OPEN (cancelled/completed): service message shown, no confirmation, menu keeps running")
        void notOpen() throws Exception {
            doThrow(new IllegalStateException("Order is already canceled or already completed."))
                    .when(service).cancelPurchaseOrder(5);
            WarehouseUI ui = uiWithInput("8", "5", "0");

            ui.run();

            assertTrue(output().contains("Order is already canceled or already completed."), output());
            assertFalse(output().contains("#5 cancelled."), output());
        }

        @Test
        @DisplayName("Unknown order: service message shown, no confirmation, menu keeps running")
        void unknownOrder() throws Exception {
            doThrow(new IllegalArgumentException("No purchase order found with ID: 999."))
                    .when(service).cancelPurchaseOrder(999);
            WarehouseUI ui = uiWithInput("8", "999", "0");

            ui.run();

            assertTrue(output().contains("No purchase order found with ID: 999."), output());
            assertFalse(output().contains("#999 cancelled."), output());
        }

        @Test
        @DisplayName("Letters instead of an id: error shown, nothing sent to the service")
        void lettersAsId() throws Exception {
            WarehouseUI ui = uiWithInput("8", "abc", "0");

            ui.run();

            verify(service, never()).cancelPurchaseOrder(anyInt());
            assertTrue(output().contains("Enter a valid number."), output());
        }

        @Test
        @DisplayName("Storage error: generic storage message shown, no confirmation")
        void storageError() throws Exception {
            doThrow(new SQLException("disk full")).when(service).cancelPurchaseOrder(5);
            WarehouseUI ui = uiWithInput("8", "5", "0");

            ui.run();

            assertTrue(output().contains("Something went wrong in the storage layer."), output());
            assertFalse(output().contains("#5 cancelled."), output());
        }
    }

    @Nested
    @Feature("Sales order")
    @Story("Cancel sales order")
    @DisplayName("Cancel sales order [10]")
    class SalesOrder {

        @Test
        @DisplayName("Open order: id goes to the service, confirmation is shown")
        void cancelOpen() throws Exception {
            WarehouseUI ui = uiWithInput("10", "5", "0");

            ui.run();

            verify(service).cancelSalesOrder(5);
            assertTrue(output().contains("Sales order #5 cancelled."), output());
        }

        @Test
        @DisplayName("Order not OPEN (cancelled/completed): service message shown, no confirmation, menu keeps running")
        void notOpen() throws Exception {
            doThrow(new IllegalStateException("Order is already canceled or already completed."))
                    .when(service).cancelSalesOrder(5);
            WarehouseUI ui = uiWithInput("10", "5", "0");

            ui.run();

            assertTrue(output().contains("Order is already canceled or already completed."), output());
            assertFalse(output().contains("#5 cancelled."), output());
        }

        @Test
        @DisplayName("Unknown order: service message shown, no confirmation, menu keeps running")
        void unknownOrder() throws Exception {
            doThrow(new IllegalArgumentException("No sales order found with ID: 999."))
                    .when(service).cancelSalesOrder(999);
            WarehouseUI ui = uiWithInput("10", "999", "0");

            ui.run();

            assertTrue(output().contains("No sales order found with ID: 999."), output());
            assertFalse(output().contains("#999 cancelled."), output());
        }

        @Test
        @DisplayName("Letters instead of an id: error shown, nothing sent to the service")
        void lettersAsId() throws Exception {
            WarehouseUI ui = uiWithInput("10", "abc", "0");

            ui.run();

            verify(service, never()).cancelSalesOrder(anyInt());
            assertTrue(output().contains("Enter a valid number."), output());
        }

        @Test
        @DisplayName("Storage error: generic storage message shown, no confirmation")
        void storageError() throws Exception {
            doThrow(new SQLException("disk full")).when(service).cancelSalesOrder(5);
            WarehouseUI ui = uiWithInput("10", "5", "0");

            ui.run();

            assertTrue(output().contains("Something went wrong in the storage layer."), output());
            assertFalse(output().contains("#5 cancelled."), output());
        }
    }
}
