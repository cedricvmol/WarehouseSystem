# Domain Model

Planned model from initial design — will be updated as classes are actually
built, starting in iteration 1.

## Product
- **Responsibility:** represents an item the warehouse stocks
- **Key fields:** id, name, reorder threshold, supplier reference
- **Relationships:** belongs to one `Supplier`; stock tracked via `StockItem`

## Supplier
- **Responsibility:** source of purchased products
- **Key fields:** id, name, contact info
- **Relationships:** supplies many `Product`s

## Bin
- **Responsibility:** a physical storage location in the warehouse
- **Key fields:** id, location/code
- **Relationships:** holds stock via `StockItem`

## StockItem
- **Responsibility:** links a Product to a Bin with a quantity — allows one
  product to be split across multiple bins
- **Key fields:** product reference, bin reference, quantity
- **Relationships:** references one `Product` and one `Bin`

## Customer
- **Responsibility:** recipient of a `SalesOrder`
- **Key fields:** id, name, contact info

## OrderStatus (enum)
- **Values:** `OPEN`, `CANCELLED`, `COMPLETED`
- **Used by:** `PurchaseOrder` and `SalesOrder`. New orders start `OPEN`.
  `COMPLETED` is not set by any flow yet (Iteration 3: receive / ship).

## PurchaseOrder
- **Responsibility:** an order placed with a Supplier to receive stock
- **Key fields:** id (DB-generated), supplier reference, orderDate, orderStatus, line items
- **Key methods:** `cancel()` — `OPEN` → `CANCELLED`, otherwise `IllegalStateException`
- **Rules:** at least one line item (constructor throws on null/empty list)
- **Constructors:** full (used when loading from DB) and short
  `(supplier, lineItems)` → id 0, now, `OPEN`
- **Relationships:** belongs to one `Supplier`; has many `PurchaseOrderLineItem`
- **Storage:** `PurchaseOrderDao` implements `Repository<PurchaseOrder, Integer>`;
  line items are saved and loaded in the same DAO, in one transaction

## PurchaseOrderLineItem
- **Responsibility:** one product + quantity within a PurchaseOrder
- **Key fields:** product reference, quantity ordered

## SalesOrder
- **Responsibility:** an order placed by a Customer to ship stock out
- **Key fields:** id (DB-generated), customer reference, orderDate, orderStatus, line items
- **Key methods / rules / constructors:** same as `PurchaseOrder`
- **Relationships:** belongs to one `Customer`; has many `SalesOrderLineItem`
- **Storage:** `SalesOrderDao` implements `Repository<SalesOrder, Integer>`

## SalesOrderLineItem
- **Responsibility:** one product + quantity within a SalesOrder
- **Key fields:** product reference, quantity ordered
