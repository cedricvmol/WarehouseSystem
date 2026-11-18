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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The "Adjust stock" CLI flow (menu [15], KAN-99) with a mocked service.
 * All tests go through run(), so the menu routing is covered too.
 * The rules themselves are in WarehouseServiceTest and AdjustStockIntegrationTest.
 */
@Tag("unit")
@Epic("CLI")
@Feature("Stock")
@Story("Adjust stock")
@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseUI - adjust stock [15] (KAN-99)")
class WarehouseUIAdjustStockTest {

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
    @DisplayName("Menu shows option [15] Adjust stock")
    void menuShowsOption() throws Exception {
        uiWithInput("0").run();

        assertTrue(output().contains("[15] Adjust stock"), output());
    }

    @Test
    @DisplayName("Product, bin and new quantity go to the service in that order, confirmation is shown")
    void happyPath() throws Exception {
        uiWithInput("15", "1", "2", "8", "0").run();

        verify(service).adjustStock(1, 2, 8);
        assertTrue(output().contains("Stock adjusted."), output());
    }

    @Test
    @DisplayName("Service rejects (no stock row / negative): message shown, no confirmation, menu keeps running")
    void serviceRejects() throws Exception {
        doThrow(new IllegalArgumentException("No stockItem found with productId: 1 and binId: 2."))
                .when(service).adjustStock(1, 2, 8);

        uiWithInput("15", "1", "2", "8", "0").run();

        assertTrue(output().contains("No stockItem found with productId: 1 and binId: 2."), output());
        assertFalse(output().contains("Stock adjusted."), output());
    }

    @Test
    @DisplayName("Letters instead of a quantity: error shown, nothing sent to the service")
    void lettersAsQuantity() throws Exception {
        uiWithInput("15", "1", "2", "abc", "0").run();

        verify(service, never()).adjustStock(anyInt(), anyInt(), anyInt());
        assertTrue(output().contains("Enter a valid number."), output());
    }

    @Test
    @DisplayName("Storage error: generic storage message shown, no confirmation")
    void storageError() throws Exception {
        doThrow(new SQLException("disk full")).when(service).adjustStock(1, 2, 8);

        uiWithInput("15", "1", "2", "8", "0").run();

        assertTrue(output().contains("Something went wrong in the storage layer."), output());
        assertFalse(output().contains("Stock adjusted."), output());
    }
}
