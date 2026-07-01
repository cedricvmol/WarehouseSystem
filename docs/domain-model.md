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

## PurchaseOrder
- **Responsibility:** an order placed with a Supplier to receive stock
- **Key fields:** id, supplier reference, date, status, line items
- **Relationships:** belongs to one `Supplier`; has many `PurchaseOrderLineItem`

## PurchaseOrderLineItem
- **Responsibility:** one product + quantity within a PurchaseOrder
- **Key fields:** product reference, quantity ordered

## SalesOrder
- **Responsibility:** an order placed by a Customer to ship stock out
- **Key fields:** id, customer reference, date, status, line items
- **Relationships:** belongs to one `Customer`; has many `SalesOrderLineItem`

## SalesOrderLineItem
- **Responsibility:** one product + quantity within a SalesOrder
- **Key fields:** product reference, quantity ordered
