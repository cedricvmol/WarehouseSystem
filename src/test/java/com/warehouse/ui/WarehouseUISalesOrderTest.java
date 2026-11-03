package com.warehouse.ui;

import com.warehouse.service.WarehouseService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The "Create sales order" CLI flow (menu [9]) with a mocked service. Mirrors
 * WarehouseUIPurchaseOrderTest: checks what the UI hands to the service and what
 * the user sees, not the order rules themselves (WarehouseServiceTest).
 */
@Tag("unit")
@Epic("CLI")
@Feature("Sales order")
@Story("Create sales order")
@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseUI - create sales order")
class WarehouseUISalesOrderTest {

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

    @Test
    @DisplayName("Via menu [9], one line item: customer and line item go to the service, the new order id is shown")
    void singleLineItemViaMenu() throws Exception {
        when(service.createSalesOrder(anyInt(), anyMap())).thenReturn(42);
        // menu [9], customer, product, quantity, [2] save, menu [0] exit
        WarehouseUI ui = uiWithInput("9", "7", "1", "3", "2", "0");

        ui.run();

        verify(service).createSalesOrder(7, Map.of(1, 3));
        assertTrue(output().contains("Sales order #42 created."), output());
    }

    @Test
    @DisplayName("Two different products: both line items go to the service")
    void twoProducts() throws Exception {
        WarehouseUI ui = uiWithInput("7", "1", "3", "1", "2", "4", "2");

        ui.createSalesOrder();

        verify(service).createSalesOrder(7, Map.of(1, 3, 2, 4));
    }

    // Summing replaces the domain rule D7 that was removed from KAN-87.
    @Test
    @DisplayName("Same product entered twice: quantities are added up into one line item")
    void sameProductTwiceIsSummed() throws Exception {
        WarehouseUI ui = uiWithInput("7", "1", "3", "1", "1", "5", "2");

        ui.createSalesOrder();

        verify(service).createSalesOrder(7, Map.of(1, 8));
    }

    @Test
    @DisplayName("Same product with other products in between: still summed, other lines untouched")
    void sameProductNotConsecutive() throws Exception {
        WarehouseUI ui = uiWithInput("7", "1", "3", "1", "2", "4", "1", "1", "2", "2");

        ui.createSalesOrder();

        verify(service).createSalesOrder(7, Map.of(1, 5, 2, 4));
    }

    @Test
    @DisplayName("Letters instead of a quantity: error shown, nothing sent to the service, back to the menu")
    void lettersAsQuantity() throws Exception {
        // menu [9], customer, product, "abc" as quantity, then menu [0] exit
        WarehouseUI ui = uiWithInput("9", "7", "1", "abc", "0");

        ui.run();

        verify(service, never()).createSalesOrder(anyInt(), any());
        assertTrue(output().contains("Enter a valid number."), output());
    }

    @Test
    @DisplayName("Service rejects the order (e.g. unknown customer): its message is shown, menu keeps running")
    void serviceRejection() throws Exception {
        when(service.createSalesOrder(eq(99), anyMap()))
                .thenThrow(new IllegalArgumentException("No customer found with ID: 99."));
        WarehouseUI ui = uiWithInput("9", "99", "1", "3", "2", "0");

        ui.run();

        assertTrue(output().contains("No customer found with ID: 99."), output());
        assertFalse(output().contains("created."), output());
    }

    @Test
    @DisplayName("Storage error: generic storage message shown, menu keeps running")
    void storageError() throws Exception {
        when(service.createSalesOrder(anyInt(), anyMap())).thenThrow(new SQLException("disk full"));
        WarehouseUI ui = uiWithInput("9", "7", "1", "3", "2", "0");

        ui.run();

        assertTrue(output().contains("Something went wrong in the storage layer."), output());
    }
}
