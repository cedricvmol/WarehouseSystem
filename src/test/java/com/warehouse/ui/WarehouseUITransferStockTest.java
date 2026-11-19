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
 * The "Transfer stock" CLI flow (menu [16], KAN-98) with a mocked service.
 * All tests go through run(), so the menu routing is covered too.
 * The rules themselves (AC1-8) are in WarehouseServiceTest and TransferStockIntegrationTest.
 */
@Tag("unit")
@Epic("CLI")
@Feature("Stock")
@Story("Transfer stock")
@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseUI - transfer stock [16] (KAN-98)")
class WarehouseUITransferStockTest {

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
    @DisplayName("Menu shows option [16] Transfer stock")
    void menuShowsOption() throws Exception {
        uiWithInput("0").run();

        assertTrue(output().contains("[16] Transfer stock"), output());
    }

    @Test
    @DisplayName("Product, from bin, to bin and quantity go to the service in that order, confirmation is shown")
    void happyPath() throws Exception {
        uiWithInput("16", "1", "2", "3", "4", "0").run();

        verify(service).transferStock(1, 2, 3, 4);
        assertTrue(output().contains("Stock transferred."), output());
    }

    @Test
    @DisplayName("Service rejects (e.g. not enough stock): message shown, no confirmation, menu keeps running")
    void serviceRejects() throws Exception {
        doThrow(new IllegalArgumentException("Not enough stock in bin 2: requested 4, available 1."))
                .when(service).transferStock(1, 2, 3, 4);

        uiWithInput("16", "1", "2", "3", "4", "0").run();

        assertTrue(output().contains("Not enough stock in bin 2: requested 4, available 1."), output());
        assertFalse(output().contains("Stock transferred."), output());
    }

    @Test
    @DisplayName("Letters instead of a quantity: error shown, nothing sent to the service")
    void lettersAsQuantity() throws Exception {
        uiWithInput("16", "1", "2", "3", "abc", "0").run();

        verify(service, never()).transferStock(anyInt(), anyInt(), anyInt(), anyInt());
        assertTrue(output().contains("Enter a valid number."), output());
    }

    @Test
    @DisplayName("Storage error: generic storage message shown, no confirmation")
    void storageError() throws Exception {
        doThrow(new SQLException("disk full")).when(service).transferStock(1, 2, 3, 4);

        uiWithInput("16", "1", "2", "3", "4", "0").run();

        assertTrue(output().contains("Something went wrong in the storage layer."), output());
        assertFalse(output().contains("Stock transferred."), output());
    }
}
