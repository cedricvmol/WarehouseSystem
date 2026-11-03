package com.warehouse.ui;

import com.warehouse.domain.Bin;
import com.warehouse.domain.Customer;
import com.warehouse.domain.Product;
import com.warehouse.domain.Supplier;
import com.warehouse.service.WarehouseService;


import java.sql.SQLException;
import java.util.*;

public class WarehouseUI {

    private WarehouseService warehouseService;
    private Scanner scanner;

    public WarehouseUI(WarehouseService warehouseService){
        this.warehouseService = warehouseService;
        this.scanner = new Scanner(System.in);
    }

    public void run(){

        while(true){
            System.out.println();
            System.out.println("╔══════════════════════════════════╗");
            System.out.println("║        WAREHOUSE  SYSTEM         ║");
            System.out.println("╚══════════════════════════════════╝");
            System.out.println("  MASTER DATA");
            System.out.println("  [1]  Add product");
            System.out.println("  [2]  Add supplier");
            System.out.println("  [3]  Add customer");
            System.out.println("  [4]  Add bin");
            System.out.println("  [11] List products");
            System.out.println("  [12] List suppliers");
            System.out.println("  [13] List customers");
            System.out.println("  [14] List bins");
            System.out.println("  ──────────────────────────────────");
            System.out.println("  STOCK");
            System.out.println("  [5]  Add stock item");
            System.out.println("  [6]  Check stock level");
            System.out.println("  ──────────────────────────────────");
            System.out.println("  PURCHASE ORDERS");
            System.out.println("  [7]  Create purchase order");
            System.out.println("  [8]  Cancel purchase order");
            System.out.println("  ──────────────────────────────────");
            System.out.println("  SALES ORDERS");
            System.out.println("  [9]  Create sales order");
            System.out.println("  [10] Cancel sales order");
            System.out.println("  ──────────────────────────────────");
            System.out.println("  [0]  Exit");
            System.out.print("\n  Choice: ");

            String input = scanner.nextLine().trim();
            try {
                switch (input) {
                    case "1" : addProduct();
                        break;

                    case "2" : addSupplier();
                        break;

                    case "3" : addCustomer();
                        break;

                    case "4" : addBin();
                        break;

                    case "5" : addStockItem();
                        break;

                    case "6" : checkStockLevel();
                        break;

                    case "7" : createPurchaseOrder();
                        break;

                    case "8" : cancelPurchaseOrder();
                        break;

                    case "9" : createSalesOrder();
                        break;

                    case "10" : cancelSalesOrder();
                        break;

                    case "11" : giveListAllProducts();
                        break;

                    case "12" : giveListAllSuppliers();
                        break;

                    case "13" : giveListAllCustomers();
                        break;

                    case "14" : giveListAllBins();
                        break;

                    case "0" : return;
                    default:
                        System.out.println("  ✘ Invalid option, try again.");
                }
            } catch (IllegalArgumentException e){
                System.out.println("  ✘ " + e.getMessage());
            } catch (InputMismatchException e) {
                System.out.println("  ✘ Enter a valid number.");
                scanner.nextLine();
            } catch (SQLException e){
                System.out.println("  ✘ Something went wrong in the storage layer.");
            } catch (IllegalStateException e){
                System.out.println("  ✘ " + e.getMessage());
            }

        }
    }

    public void addProduct() throws SQLException {
        System.out.println("\n  ── ADD PRODUCT ───────────────────");

        System.out.print("  ID:             ");
        int productId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("  Name:           ");
        String name = scanner.nextLine().trim();

        System.out.print("  Threshold:      ");
        int threshold = scanner.nextInt();
        scanner.nextLine();

        System.out.print("  Supplier ID:    ");
        int supplierId = scanner.nextInt();
        scanner.nextLine();

        warehouseService.addProduct(productId,name,threshold,supplierId);
        System.out.println("  ✔ Product added.");
    }

    public void addSupplier() throws SQLException {
        System.out.println("\n  ── ADD SUPPLIER ──────────────────");

        System.out.print("  ID:             ");
        int supplierId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("  Name:           ");
        String name = scanner.nextLine().trim();

        System.out.print("  Email:          ");
        String email = scanner.nextLine().trim();

        warehouseService.addSupplier(supplierId,name,email);
        System.out.println("  ✔ Supplier added.");
    }

    public void addCustomer() throws SQLException {
        System.out.println("\n  ── ADD CUSTOMER ──────────────────");

        System.out.print("  ID:             ");
        int customerId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("  Name:           ");
        String name = scanner.nextLine().trim();

        System.out.print("  Email:          ");
        String email = scanner.nextLine().trim();

        warehouseService.addCustomer(customerId,name,email);
        System.out.println("  ✔ Customer added.");
    }

    public void addBin() throws SQLException {
        System.out.println("\n  ── ADD BIN ───────────────────────");

        System.out.print("  ID:             ");
        int binId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("  Location code:  ");
        String locationCode = scanner.nextLine().trim();


        warehouseService.addBin(binId,locationCode);
        System.out.println("  ✔ Bin added.");
    }

    public void addStockItem() throws SQLException {
        System.out.println("\n  ── ADD STOCK ITEM ────────────────");

        System.out.print("  Product ID:     ");
        int productId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("  Bin ID:         ");
        int binId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("  Quantity:       ");
        int quantity = scanner.nextInt();
        scanner.nextLine();

        warehouseService.addStockItem(productId,binId,quantity);
        System.out.println("  ✔ Stock item added.");
    }

    public void checkStockLevel() throws SQLException {
        System.out.println("\n  ── CHECK STOCK LEVEL ─────────────");

        System.out.print("  Product ID:     ");
        int productId = scanner.nextInt();
        scanner.nextLine();

        int stockLevel = warehouseService.checkStockLevel(productId);

        System.out.println("  Stock level:    " + stockLevel);
    }

    public void createPurchaseOrder() throws SQLException{
        Map<Integer,Integer> lineItems = new HashMap<>();

        System.out.println("\n  ── CREATE PURCHASE ORDER ─────────");
        System.out.print("  Supplier ID:    ");
        int supplierId = scanner.nextInt();
        scanner.nextLine();

        while(true){
            System.out.println("\n  ·· Line item ·····················");

            System.out.print("  Product ID:     ");
            int productId = scanner.nextInt();
            scanner.nextLine();

            System.out.print("  Quantity:       ");
            int quantity = scanner.nextInt();
            scanner.nextLine();

            System.out.println("  ──────────────────────────────────");
            System.out.println("  [1]  Add another product");
            System.out.println("  [2]  Save order");

            System.out.print("  Choice:         ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            int totalAmount = lineItems.getOrDefault(productId,0) + quantity;
            lineItems.put(productId,totalAmount);

            if(choice == 2){
                break;
            }
        }
        int orderId = warehouseService.createPurchaseOrder(supplierId,lineItems);
        System.out.println("  ✔ Purchase order #" + orderId + " created.");
    }

    public void createSalesOrder() throws SQLException {
        Map<Integer,Integer> lineItems = new HashMap<>();

        System.out.println("\n  ── CREATE SALES ORDER ────────────");
        System.out.print("  Customer ID:    ");
        int customerId = scanner.nextInt();
        scanner.nextLine();

        while(true){
            System.out.println("\n  ·· Line item ·····················");

            System.out.print("  Product ID:     ");
            int productId = scanner.nextInt();
            scanner.nextLine();

            System.out.print("  Quantity:       ");
            int quantity = scanner.nextInt();
            scanner.nextLine();

            System.out.println("  ──────────────────────────────────");
            System.out.println("  [1]  Add another product");
            System.out.println("  [2]  Save order");

            System.out.print("  Choice:         ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            int totalAmount = lineItems.getOrDefault(productId,0) + quantity;
            lineItems.put(productId,totalAmount);

            if(choice == 2){
                break;
            }
        }
        int orderId = warehouseService.createSalesOrder(customerId,lineItems);
        System.out.println("  ✔ Sales order #" + orderId + " created.");
    }


    public void cancelPurchaseOrder() throws SQLException{
        System.out.println("\n  ── CANCEL PURCHASE ORDER ─────────");

        System.out.print("  Order ID:       ");
        int orderId = scanner.nextInt();
        scanner.nextLine();

        warehouseService.cancelPurchaseOrder(orderId);
        System.out.println("  ✔ Purchase order #" + orderId + " cancelled.");
    }

    public void cancelSalesOrder() throws SQLException{
        System.out.println("\n  ── CANCEL SALES ORDER ────────────");

        System.out.print("  Order ID:       ");
        int orderId = scanner.nextInt();
        scanner.nextLine();

        warehouseService.cancelSalesOrder(orderId);
        System.out.println("  ✔ Sales order #" + orderId + " cancelled.");
    }

    public void giveListAllProducts() throws SQLException{
        List<Product> products = warehouseService.giveListAllProducts();

        if(!products.isEmpty()){

            System.out.println("\n  ── PRODUCTS ──────────────────────────────────────────");
            System.out.printf("  %-5s %-22s %-9s %-20s%n", "ID", "Name", "Threshold", "Supplier");
            System.out.println("  ─────────────────────────────────────────────────────");

            for(Product product : products){
                System.out.printf("  %-5d %-22s %-9d %-20s%n", product.getProductId(), product.getName(),product.getThreshold(), product.getSupplier().getName());
            }
        }else {
            System.out.println("  No products found.");
        }

    }

    public void giveListAllSuppliers() throws SQLException{
        List<Supplier> suppliers = warehouseService.giveListAllSuppliers();

        if(!suppliers.isEmpty()){
            System.out.println("\n  ── SUPPLIERS ─────────────────────────────────────────");
            System.out.printf("  %-5s %-22s %-28s%n", "ID", "Name", "Email");
            System.out.println("  ─────────────────────────────────────────────────────");

            for(Supplier supplier : suppliers){
                System.out.printf("  %-5d %-22s %-28s%n", supplier.getSupplierId(), supplier.getName(), supplier.getEmail());
            }
        }else {
            System.out.println("  No suppliers found.");
        }

    }

    public void giveListAllCustomers() throws SQLException{
        List<Customer> customers = warehouseService.giveListAllCustomers();

        if(!customers.isEmpty()){
            System.out.println("\n  ── CUSTOMERS ─────────────────────────────────────────");
            System.out.printf("  %-5s %-22s %-28s%n", "ID", "Name", "Email");
            System.out.println("  ─────────────────────────────────────────────────────");

            for(Customer customer : customers){
                System.out.printf("  %-5d %-22s %-28s%n",customer.getCustomerId(),customer.getName(),customer.getEmail());
            }
        }else {
            System.out.println("  No customers found.");
        }

    }



    public void giveListAllBins() throws SQLException{
        List<Bin> bins = warehouseService.giveListAllBins();

        if(!bins.isEmpty()){
            System.out.println("\n  ── BINS ──────────────────────────");
            System.out.printf("  %-5s %-15s%n", "ID", "Location Code");
            System.out.println("  ─────────────────────────────────");

            for(Bin bin : bins){
                System.out.printf("  %-5d %-15s%n", bin.getBinId(),bin.getLocationCode());
            }
        }else {
            System.out.println("  No bins found.");
        }

    }
}
