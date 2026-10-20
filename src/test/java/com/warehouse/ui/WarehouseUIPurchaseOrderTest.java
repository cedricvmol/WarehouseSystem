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
 * The "Create purchase order" CLI flow with a mocked service: typed input goes
 * in via System.in, what the user sees comes out via System.out. Checks what the
 * UI hands to the service, not the order rules themselves (WarehouseServiceTest).
 */
@Tag("unit")
@Epic("CLI")
@Feature("Purchase order")
@Story("Create purchase order")
@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseUI - create purchase order")
class WarehouseUIPurchaseOrderTest {

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
    @DisplayName("One line item: supplier and line item go to the service, the new order id is shown")
    void singleLineItem() throws Exception {
        when(service.createPurchaseOrder(anyInt(), anyMap())).thenReturn(42);
        // supplier, product, quantity, [2] save
        WarehouseUI ui = uiWithInput("7", "1", "3", "2");

        ui.createPurchaseOrder();

        verify(service).createPurchaseOrder(7, Map.of(1, 3));
        assertTrue(output().contains("Purchase order #42 created."), output());
    }

    @Test
    @DisplayName("Two different products: both line items go to the service")
    void twoProducts() throws Exception {
        WarehouseUI ui = uiWithInput("7", "1", "3", "1", "2", "4", "2");

        ui.createPurchaseOrder();

        verify(service).createPurchaseOrder(7, Map.of(1, 3, 2, 4));
    }

    @Test
    @DisplayName("Same product entered twice: quantities are added up into one line item")
    void sameProductTwiceIsSummed() throws Exception {
        WarehouseUI ui = uiWithInput("7", "1", "3", "1", "1", "5", "2");

        ui.createPurchaseOrder();

        verify(service).createPurchaseOrder(7, Map.of(1, 8));
    }

    @Test
    @DisplayName("Same product with other products in between: still summed, other lines untouched")
    void sameProductNotConsecutive() throws Exception {
        WarehouseUI ui = uiWithInput("7", "1", "3", "1", "2", "4", "1", "1", "2", "2");

        ui.createPurchaseOrder();

        verify(service).createPurchaseOrder(7, Map.of(1, 5, 2, 4));
    }

    @Test
    @DisplayName("Letters instead of a quantity: error shown, nothing sent to the service, back to the menu")
    void lettersAsQuantity() throws Exception {
        // menu [7], supplier, product, "abc" as quantity, then menu [0] exit
        WarehouseUI ui = uiWithInput("7", "7", "1", "abc", "0");

        ui.run();

        verify(service, never()).createPurchaseOrder(anyInt(), any());
        assertTrue(output().contains("Enter a valid number."), output());
    }

    @Test
    @DisplayName("Service rejects the order (e.g. unknown supplier): its message is shown, menu keeps running")
    void serviceRejection() throws Exception {
        when(service.createPurchaseOrder(eq(99), anyMap()))
                .thenThrow(new IllegalArgumentException("No supplier found with ID: 99."));
        WarehouseUI ui = uiWithInput("7", "99", "1", "3", "2", "0");

        ui.run();

        assertTrue(output().contains("No supplier found with ID: 99."), output());
        assertFalse(output().contains("created."), output());
    }

    @Test
    @DisplayName("Storage error: generic storage message shown, menu keeps running")
    void storageError() throws Exception {
        when(service.createPurchaseOrder(anyInt(), anyMap())).thenThrow(new SQLException("disk full"));
        WarehouseUI ui = uiWithInput("7", "7", "1", "3", "2", "0");

        ui.run();

        assertTrue(output().contains("Something went wrong in the storage layer."), output());
    }
}
