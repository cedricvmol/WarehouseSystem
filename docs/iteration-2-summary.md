# Iteration 2 – Orders & Line Items

## What was built
- Domain: `PurchaseOrder`, `PurchaseOrderLineItem`, `SalesOrder`, `SalesOrderLineItem`
  and the `OrderStatus` enum (`OPEN`, `CANCELLED`, `COMPLETED`). Orders reject an
  empty or null line item list in the constructor; `cancel()` only works on an
  `OPEN` order (`IllegalStateException` otherwise).
- Two constructors per order: a full one (used by the DAO when loading) and a
  short one `(supplier/customer, lineItems)` that sets id 0, `LocalDateTime.now()`
  and `OPEN` for new orders.
- Schema: `purchaseOrders`, `purchaseOrderLineItems`, `salesOrders`,
  `salesOrderLineItems`. Order IDs are `INTEGER PRIMARY KEY AUTOINCREMENT`;
  line items have a composite key (orderId + productId).
- Generic `Repository<T, ID>` interface (`insert`, `findById`, `findAll`,
  `update`, `delete`), implemented by `PurchaseOrderDao` and `SalesOrderDao`
  as `Repository<…, Integer>`. Line items are persisted inside the order DAO.
- `insert` returns the DB-generated ID via `getGeneratedKeys()`.
- Transactions: order `insert`, `update` and `delete` run in one transaction
  (`setAutoCommit(false)` → `commit` / `rollback` in catch → `setAutoCommit(true)`
  in finally), so a failing line item never leaves an orphan order header
  (KAN-92, KAN-93).
- `DatabaseManager(String url)` added next to the default constructor, so tests
  can use their own SQLite file (KAN-90). `PRAGMA foreign_keys = ON` enforced on
  every connection (KAN-91).
- `WarehouseService`: `createPurchaseOrder`, `createSalesOrder`,
  `cancelPurchaseOrder`, `cancelSalesOrder`. Create methods take
  `Map<Integer, Integer>` (productId → quantity) and return the new order ID.
- `WarehouseUI`: menu restyled into sections; options [7]–[10] for create/cancel
  of both order types. Same product entered twice is merged into one line item.
- Tests (written via /qa): domain, service (Mockito), DAO integration tests
  against a temporary DB, UI tests for the order screens. Manual run
  `Iteration 2 – Acceptance` in Testiny: 18/18 passed.

## Key decisions
- DB-generated order IDs (`AUTOINCREMENT`) instead of user-entered IDs, because
  order numbers are business identifiers that must never be reused. The five
  master-data tables keep user-entered IDs for now (KAN-77, Iteration 5).
- Existing Iteration 1 DAOs are not retrofitted onto `Repository<T, ID>` yet —
  deferred to Iteration 5 to keep this iteration scoped to orders.
- Line items get no DAO of their own: composite key doesn't fit `Repository<T, ID>`,
  and they have no life outside their order.
- Cancel keeps the line items in the DB — only the status changes.
- Manual tests run against `warehouse.db` with existing data; shared base data
  comes from `src/test/resources/manual-test-data.sql` (DB is not in git).
- `createSalesOrder()` in the UI was written out in full instead of extracting a
  shared line-item helper; the duplication is a candidate for Iteration 5.

## Concepts practiced
- **Generics (new):** `Repository<T, ID>` with two type parameters, implemented
  with concrete types (`Repository<PurchaseOrder, Integer>`).
- **JDBC transactions (new):** manual commit/rollback for multi-statement writes.
- `getGeneratedKeys()` with `RETURN_GENERATED_KEYS` to read back auto IDs.
- Enums for a fixed set of states (`OrderStatus`), state checks in the domain.
- Constructor chaining with `this(...)` for default values.
- `Map.getOrDefault` to merge quantities for duplicate products.
- Throw in domain/service, catch in `WarehouseUI.run()` (`IllegalStateException`
  added to the catch list).

## Known limitations / next steps
- **KAN-88** — `getPurchaseOrderLineItems()` / `getSalesOrderLineItems()` return
  the internal list; callers can `clear()` it and bypass the "at least one line
  item" rule (needs a defensive/unmodifiable copy).
- **KAN-89** — `null` line items in the service throw an unhandled
  `NullPointerException` before the domain check is reached.
- **KAN-94** — a foreign key violation shows the generic "storage layer" message.
- **KAN-87** — no input validation yet on quantities, thresholds, names, e-mail.
- **KAN-95** — no way to list master data; users must know IDs by heart.
- Orders have no effect on stock yet — receiving and shipping are Iteration 3.
- `COMPLETED` exists in the enum but nothing sets it yet (only via SQL in tests).
