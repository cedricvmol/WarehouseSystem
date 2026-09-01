# WarehouseSystem — Project Overview

## Goal
CLI application managing inventory for a single warehouse: products, storage
bins, suppliers, customers, purchase/sales orders, and stock movement between
bins. Fourth project in the Java learning series (after BankApp, LibrarySystem).

## Domain Summary
A warehouse stores **Products** across multiple **Bins**, tracked via
**StockItem** records (product + bin + quantity). Stock arrives through a
**PurchaseOrder** (from a **Supplier**) and leaves through a **SalesOrder**
(to a **Customer**). Each order type has its own line item class. A single
actor ("Warehouse Staff") performs all operations — no role separation.

See `domain-model.md` for class-level detail.

## Architecture
Layered architecture, consistent with prior projects:
- **Domain** — Product, Supplier, Bin, Customer, StockItem, PurchaseOrder,
  SalesOrder, PurchaseOrderLineItem, SalesOrderLineItem
- **Service layer** — `WarehouseService` as the single facade the UI calls;
  individual services (e.g. StockService, OrderService) behind it
- **Storage** — SQLite via JDBC, one DAO per entity, from iteration 1 (no
  "add database later")
- **UI** — CLI, input/output only, no business logic

## Use Cases
**Catalog/setup:** add product, add supplier, add customer, add bin
**Stock movement:** receive stock, ship order, transfer stock between bins,
adjust stock
**Orders:** create purchase order, create sales order, cancel order
**Reporting:** check stock level, list low-stock products, view order history

## Iteration Plan

| # | Theme | New Concept(s) | Status |
|---|-------|----------------|--------|
| 1 | Core Domain & Master Data | — (reinforcement: OOP, service layer, SQLite/JDBC) | Done |
| 2 | Orders & Line Items | Generics (generic `Repository<T, ID>` DAO base) | Not started |
| 3 | Fulfillment & Stock Movement | Strategy pattern (bin-picking strategy for shipping) | Not started |
| 4 | Low-Stock Alerts & Reporting | Observer pattern (reorder-threshold notifications) | Not started |
| 5 | Polish & Refactor | Logging (SLF4J/Logback), Configuration (properties file) | Not started |
