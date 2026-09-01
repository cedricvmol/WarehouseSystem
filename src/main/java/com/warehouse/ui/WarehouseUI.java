package com.warehouse.ui;

import com.warehouse.service.WarehouseService;


import java.sql.SQLException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class WarehouseUI {

    private WarehouseService warehouseService;
    private Scanner scanner;

    public WarehouseUI(WarehouseService warehouseService){
        this.warehouseService = warehouseService;
        this.scanner = new Scanner(System.in);
    }

    public void run(){

        while(true){
            System.out.println("\n──────── MAIN MENU ─────────");
            System.out.println("[1] Add product.");
            System.out.println("[2] Add supplier.");
            System.out.println("[3] Add customer.");
            System.out.println("[4] Add bin.");
            System.out.println("[5] Add stock item.");
            System.out.println("[6] Check stock level.");
            System.out.println("[0] Exit");

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

                    case "0" : return;
                    default:
                        System.out.println("Invalid option, try again.");
                }
            } catch (IllegalArgumentException e){
                System.out.println(e.getMessage());
            } catch (InputMismatchException e) {
                System.out.println("Enter a valid number.");
                scanner.nextLine();
            } catch (SQLException e){
                System.out.println("Something went wrong in the storage layer.");
            }

        }
    }

    public void addProduct() throws SQLException {
        System.out.println("──────────────── ADD PRODUCT ───");

        System.out.print("ID: ");
        int productId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Threshold: ");
        int threshold = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Supplier ID: ");
        int supplierId = scanner.nextInt();
        scanner.nextLine();

        warehouseService.addProduct(productId,name,threshold,supplierId);
    }

    public void addSupplier() throws SQLException {
        System.out.println("──────────────── ADD SUPPLIER ───");

        System.out.println("ID: \n");
        int supplierId = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Name: \n");
        String name = scanner.nextLine().trim();

        System.out.println("Email: \n");
        String email = scanner.nextLine().trim();

        warehouseService.addSupplier(supplierId,name,email);
    }

    public void addCustomer() throws SQLException {
        System.out.println("──────────────── ADD CUSTOMER ───");

        System.out.println("ID: \n");
        int customerId = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Name: \n");
        String name = scanner.nextLine().trim();

        System.out.println("Email: \n");
        String email = scanner.nextLine().trim();

        warehouseService.addCustomer(customerId,name,email);
    }

    public void addBin() throws SQLException {
        System.out.println("──────────────── ADD BIN ───");

        System.out.println("ID: \n");
        int binId = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Location code: \n");
        String locationCode = scanner.nextLine().trim();


        warehouseService.addBin(binId,locationCode);
    }

    public void addStockItem() throws SQLException {
        System.out.println("──────────────── ADD STOCK ITEM ───");

        System.out.println("Product ID: \n");
        int productId = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Bin ID: \n");
        int binId = scanner.nextInt();
        scanner.nextLine();

        System.out.println("Quantity: \n");
        int quantity = scanner.nextInt();
        scanner.nextLine();

        warehouseService.addStockItem(productId,binId,quantity);
    }

    public void checkStockLevel() throws SQLException {
        System.out.println("──────────────── CHECK STOCK LEVEL ───");

        System.out.println("Product ID: \n");
        int productId = scanner.nextInt();
        scanner.nextLine();

        int stockLevel = warehouseService.checkStockLevel(productId);

        System.out.println("The stock level is: " + stockLevel + ".");
    }



}
