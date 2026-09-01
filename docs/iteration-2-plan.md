# Iteration 2 Plan – Orders & Line Items

## Theme
Introduce purchase and sales orders as first-class domain concepts, with line
items. First iteration to introduce a genuinely new concept (Generics) since
project kickoff — Iteration 1 was pure reinforcement.

## Use Cases In Scope
1. Create purchase order (supplier + one or more product/quantity line items)
2. Create sales order (customer + one or more product/quantity line items)
3. Cancel order (purchase or sales)

## Out of Scope (deferred)
- Stock quantity effects of orders (receive stock, ship order) — Iteration 3
- Stock movement between bins, low-stock alerts — Iterations 3–4
- Retrofitting Iteration 1's 5 DAOs onto the generic Repository interface — Iteration 5 (Polish & Refactor)

## Domain Model
- `PurchaseOrder` — orderId, supplier reference, orderDate, status, line items
- `PurchaseOrderLineItem` — product reference, quantity
- `SalesOrder` — orderId, customer reference, orderDate, status, line items
- `SalesOrderLineItem` — product reference, quantity
- `OrderStatus` enum — fixed set of order states (exact values, e.g. `OPEN`/`CANCELLED`/`COMPLETED`, to be settled during implementation)

## Schema
Relational, FK-based, same style as `stockItems` in Iteration 1:
- `purchase_orders` (orderId PK, supplierId FK, orderDate, status)
- `purchase_order_line_items` (orderId FK, productId FK, quantity)
- `sales_orders` (orderId PK, customerId FK, orderDate, status)
- `sales_order_line_items` (orderId FK, productId FK, quantity)

## Architecture Decisions
- **New concept — Generics:** a `Repository<T, ID>` interface (`insert`, `findById`, `findAll`, `update`, `delete`). `PurchaseOrderDao` and `SalesOrderDao` implement `Repository<T, Integer>` — single int order ID fits cleanly.
- Existing 5 DAOs from Iteration 1 are **not** retrofitted onto `Repository<T, ID>` this iteration — deliberately deferred to Iteration 5 to keep this iteration scoped to Orders.
- Line items are **not** given their own generic DAO — like `StockItem` in Iteration 1, they have a composite key (orderId + productId) that doesn't fit a single-ID interface. They're persisted inside the parent order DAO (`insert`/`findById` compose line items directly), mirroring how `ProductDao` already composes its `Supplier`.
- Order status modeled as an enum — reinforces the `TransactionType` pattern from BankApp, not a new concept.
- Service methods validate referenced IDs (supplier/customer/product) the same way `addProduct` does — no redundant null-guards this time, since the Iteration 1 finding showed the DAOs already throw on a missing ID.

## Service Layer
- `createPurchaseOrder(supplierId, lineItems)`
- `createSalesOrder(customerId, lineItems)`
- `cancelPurchaseOrder(orderId)`
- `cancelSalesOrder(orderId)`

## UI
New menu options for the four operations above — I/O only, raw args passed through to the service, same discipline as Iteration 1.

## Suggested Task Order
1. Domain classes: `PurchaseOrder`, `PurchaseOrderLineItem`, `SalesOrder`, `SalesOrderLineItem`, `OrderStatus` enum
2. Schema: add the 4 new tables to `DatabaseManager`
3. Define the generic `Repository<T, ID>` interface
4. `PurchaseOrderDao` implementing `Repository<PurchaseOrder, Integer>`, composing line items
5. `SalesOrderDao` implementing `Repository<SalesOrder, Integer>`, same shape
6. `WarehouseService`: the four create/cancel methods
7. `WarehouseUI`: menu wiring for the four new operations
8. Manual test pass + review

## Concepts Practiced
**New:** Generics (`Repository<T, ID>` interface)
**Reinforced:** enums, service layer/facade pattern, throw-in-service validation, JDBC/SQLite DAO pattern, Scanner input handling
