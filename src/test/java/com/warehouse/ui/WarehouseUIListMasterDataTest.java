package com.warehouse.ui;

import com.warehouse.domain.Bin;
import com.warehouse.domain.Customer;
import com.warehouse.domain.Product;
import com.warehouse.domain.Supplier;
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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * KAN-95 "List master data" (menu [11]-[14]) with a mocked service. All tests go
 * through run(), so the menu routing is covered too. The empty-list criteria
 * (AC2, AC6) are only tested here, not manually (test-strategy 2026-11-03).
 */
@Tag("unit")
@Epic("CLI")
@Feature("Master data")
@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseUI - list master data (KAN-95)")
class WarehouseUIListMasterDataTest {

    @Mock private WarehouseService service;

    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private final Supplier partsInc = new Supplier(7, "Parts Inc", "parts@example.com");
    private final Supplier boxCo = new Supplier(8, "Box Co", "sales@boxco.test");

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

    /**
     * Asserts that one output line holds exactly these cells, in this order,
     * separated by whitespace. Independent of the column widths.
     */
    private void assertRow(Object... cells) {
        String regex = Arrays.stream(cells)
                .map(cell -> Pattern.quote(String.valueOf(cell)))
                .collect(Collectors.joining("\\s+", "^\\s*", "\\s*$"));
        Pattern row = Pattern.compile(regex);
        boolean found = output().lines().anyMatch(line -> row.matcher(line).matches());
        assertTrue(found, "No row " + Arrays.toString(cells) + " in output:\n" + output());
    }

    @Nested
    @Story("List products")
    @DisplayName("List products [11]")
    class Products {

        @Test
        @DisplayName("AC1: every product is shown with ID, name, threshold and supplier name")
        void showsEveryProduct() throws Exception {
            when(service.giveListAllProducts()).thenReturn(List.of(
                    new Product(1, "Widget", 5, partsInc),
                    new Product(2, "Gadget", 10, boxCo)));
            WarehouseUI ui = uiWithInput("11", "0");

            ui.run();

            assertRow(1, "Widget", 5, "Parts Inc");
            assertRow(2, "Gadget", 10, "Box Co");
        }

        @Test
        @DisplayName("AC2: no products gives a 'No products found.' message and no empty table")
        void emptyList() throws Exception {
            when(service.giveListAllProducts()).thenReturn(List.of());
            WarehouseUI ui = uiWithInput("11", "0");

            ui.run();

            assertTrue(output().contains("No products found."), output());
            assertFalse(output().contains("Threshold"), "Table header shown for an empty list:\n" + output());
        }

        @Test
        @DisplayName("Storage error: generic storage message, menu keeps running")
        void storageError() throws Exception {
            when(service.giveListAllProducts()).thenThrow(new SQLException("disk full"));
            WarehouseUI ui = uiWithInput("11", "0");

            ui.run();

            assertTrue(output().contains("Something went wrong in the storage layer."), output());
        }
    }

    @Nested
    @Story("List suppliers")
    @DisplayName("List suppliers [12]")
    class Suppliers {

        @Test
        @DisplayName("AC3: every supplier is shown with ID, name and email")
        void showsEverySupplier() throws Exception {
            when(service.giveListAllSuppliers()).thenReturn(List.of(partsInc, boxCo));
            WarehouseUI ui = uiWithInput("12", "0");

            ui.run();

            assertRow(7, "Parts Inc", "parts@example.com");
            assertRow(8, "Box Co", "sales@boxco.test");
        }

        @Test
        @DisplayName("AC6: no suppliers gives a 'No suppliers found.' message and no empty table")
        void emptyList() throws Exception {
            when(service.giveListAllSuppliers()).thenReturn(List.of());
            WarehouseUI ui = uiWithInput("12", "0");

            ui.run();

            assertTrue(output().contains("No suppliers found."), output());
            assertFalse(output().contains("Email"), "Table header shown for an empty list:\n" + output());
        }

        @Test
        @DisplayName("Storage error: generic storage message, menu keeps running")
        void storageError() throws Exception {
            when(service.giveListAllSuppliers()).thenThrow(new SQLException("disk full"));
            WarehouseUI ui = uiWithInput("12", "0");

            ui.run();

            assertTrue(output().contains("Something went wrong in the storage layer."), output());
        }
    }

    @Nested
    @Story("List customers")
    @DisplayName("List customers [13]")
    class Customers {

        @Test
        @DisplayName("AC4: every customer is shown with ID, name and email")
        void showsEveryCustomer() throws Exception {
            when(service.giveListAllCustomers()).thenReturn(List.of(
                    new Customer(1, "Acme", "acme@example.com"),
                    new Customer(2, "Globex", "buy@globex.test")));
            WarehouseUI ui = uiWithInput("13", "0");

            ui.run();

            assertRow(1, "Acme", "acme@example.com");
            assertRow(2, "Globex", "buy@globex.test");
        }

        @Test
        @DisplayName("AC6: no customers gives a 'No customers found.' message and no empty table")
        void emptyList() throws Exception {
            when(service.giveListAllCustomers()).thenReturn(List.of());
            WarehouseUI ui = uiWithInput("13", "0");

            ui.run();

            assertTrue(output().contains("No customers found."), output());
            assertFalse(output().contains("Email"), "Table header shown for an empty list:\n" + output());
        }

        @Test
        @DisplayName("Storage error: generic storage message, menu keeps running")
        void storageError() throws Exception {
            when(service.giveListAllCustomers()).thenThrow(new SQLException("disk full"));
            WarehouseUI ui = uiWithInput("13", "0");

            ui.run();

            assertTrue(output().contains("Something went wrong in the storage layer."), output());
        }
    }

    @Nested
    @Story("List bins")
    @DisplayName("List bins [14]")
    class Bins {

        @Test
        @DisplayName("AC5: every bin is shown with ID and location code")
        void showsEveryBin() throws Exception {
            when(service.giveListAllBins()).thenReturn(List.of(
                    new Bin(1, "A-01-01"),
                    new Bin(2, "A-01-02")));
            WarehouseUI ui = uiWithInput("14", "0");

            ui.run();

            assertRow(1, "A-01-01");
            assertRow(2, "A-01-02");
        }

        @Test
        @DisplayName("AC6: no bins gives a 'No bins found.' message and no empty table")
        void emptyList() throws Exception {
            when(service.giveListAllBins()).thenReturn(List.of());
            WarehouseUI ui = uiWithInput("14", "0");

            ui.run();

            assertTrue(output().contains("No bins found."), output());
            assertFalse(output().contains("Location"), "Table header shown for an empty list:\n" + output());
        }

        @Test
        @DisplayName("Storage error: generic storage message, menu keeps running")
        void storageError() throws Exception {
            when(service.giveListAllBins()).thenThrow(new SQLException("disk full"));
            WarehouseUI ui = uiWithInput("14", "0");

            ui.run();

            assertTrue(output().contains("Something went wrong in the storage layer."), output());
        }
    }
}
