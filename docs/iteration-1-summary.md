# Iteration 1 – Core Domain & Master Data

## What was built
- Domain classes: `Product`, `Supplier`, `Bin`, `Customer`, `StockItem` (fields + constructor + getters, no behavior)
- SQLite schema (`DatabaseManager`): `products`, `suppliers`, `bins`, `customers`, `stockItems` tables, with `stockItems` carrying FKs to `products` and `bins`
- One DAO per entity (`ProductDao`, `SupplierDao`, `BinDao`, `CustomerDao`, `StockItemDao`) with `insert`/`findById`/`findAll`/`update`/`delete`, built on `PreparedStatement` and try-with-resources
- `WarehouseService` facade exposing `addProduct`, `addSupplier`, `addCustomer`, `addBin`, `addStockItem`, `checkStockLevel` — the only entry point the UI calls
- `WarehouseUI`: menu-driven CLI wired entirely through `WarehouseService`
- `WarehouseApp`: wires DAOs → service → UI and boots the app

## Key decisions
- Service methods take raw primitive arguments (id/name/threshold/etc.) rather than pre-built domain objects — the service owns entity lookup and object construction. Initial version had the UI calling `findSupplier`/`findProduct`/`findBin` directly and assembling domain objects itself; refactored mid-iteration to keep that logic out of the UI.
- Error handling centralized in `WarehouseUI.run()`: one try-catch around the menu dispatch catches `InputMismatchException`, `IllegalArgumentException`, and `SQLException` for every operation, instead of duplicating catch blocks across each `add*` method.
- `checkStockLevel` validates the product exists before summing quantities, for consistency with the add operations, rather than silently returning 0 for an unknown product ID.

## Concepts practiced
- Service layer / facade pattern — `WarehouseService` as the single entry point for the UI (carried over from BankApp/LibrarySystem)
- Throw-in-service, catch-in-UI with `IllegalArgumentException` for invalid references (from BankApp)
- `InputMismatchException` handling with Scanner buffer clearing (`scanner.nextLine()` in the catch block) to prevent a stale token from corrupting the next loop iteration
- SQLite/JDBC: `PreparedStatement`, try-with-resources, one DAO per entity, FK constraints (from LibrarySystem)

## Known limitations / next steps
- **Redundant validation:** `ProductDao.findById`, `SupplierDao.findById`, and `BinDao.findById` already throw `IllegalArgumentException` when a row isn't found — they never return `null`. This makes the `null`-guard clauses in `WarehouseService.addProduct`, `addStockItem`, and `checkStockLevel` currently unreachable; the DAO throws first. Worth resolving before Iteration 2 — either drop the redundant service-side guards, or have the DAOs return `null` and let the service own validation as originally intended.
- Orders (`PurchaseOrder`/`SalesOrder`), stock movement between bins, and low-stock alerts are out of scope for this iteration — planned for Iterations 2–4 per `project-overview.md`.
- `update`/`delete` already exist on every DAO but aren't exposed through `WarehouseService` or the UI yet.
