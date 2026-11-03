# Iteration 3 Plan – Fulfillment & Stock Movement

## Theme
Orders start to affect stock: receiving a purchase order adds stock, shipping a
sales order removes it. Stock can also be moved between bins and corrected
after a count. Plus the open bugs and stories carried over from Iteration 2.

First iteration in the new Jira structure: one Story per use case with
Given/When/Then acceptance criteria, Taak for technical work, label
`iteration-3`, parent epic KAN-66.

## Tickets

### New use cases
| Key | Story | Notes |
|---|---|---|
| KAN-96 | Receive purchase order | Whole order into **one** bin; full receipt only; PO → COMPLETED |
| KAN-97 | Ship sales order | Picks via `PickingStrategy`; insufficient stock → error, nothing changes; SO → COMPLETED |
| KAN-98 | Transfer stock between bins | Total stock of the product unchanged |
| KAN-99 | Adjust stock (inventory correction) | Only for an existing stock row; new quantity ≥ 0 |

### Technical tasks
| Key | Taak |
|---|---|
| KAN-100 | `PickingStrategy` interface + `LargestQuantityFirstStrategy` (default) and `SmallestQuantityFirstStrategy` |
| KAN-101 | Service-level transaction for receive / ship / transfer across DAOs |

### Carried over from Iteration 2
| Key | Type | Summary |
|---|---|---|
| KAN-87 | Story | Reject invalid order and stock input (D1, D2, D6, D7) |
| KAN-102 | Story | Reject invalid master data input (D3, D4, D5, D9) — split from KAN-87 |
| KAN-88 | Bug | Order line items can be modified after creation |
| KAN-89 | Bug | `null` line items → unhandled `NullPointerException` |
| KAN-94 | Bug | Foreign key violation shows generic "storage layer" error — **closed, not reproducible** (DAO `findById` already rejected unknown IDs) |
| KAN-95 | Story | List master data (products, suppliers, customers, bins) |

### Found during Iteration 3
| Key | Type | Summary |
|---|---|---|
| KAN-103 | Bug | Duplicate ID on add product/supplier/customer/bin shows generic "storage layer" error — split from KAN-94 |

## Open to-dos
- [ ] **KAN-103 — manual test cases in Testiny** (folder master data, title
  `[KAN-103] ...`, SQL check as last step): duplicate ID rejected for product,
  supplier, customer and bin; new ID still added. Automated service tests
  exist (`WarehouseServiceTest$DuplicateId`).
- [ ] **Duplicate stock row** — `[5] Add stock item` for an existing
  product + bin breaks `PRIMARY KEY(productId, binId)` → generic storage
  message. Decide the behaviour (error or add quantity) in KAN-99 / KAN-98.
- [ ] **`Optional` inconsistency** — master data DAOs return `Optional` from
  `findById` (KAN-103); `PurchaseOrderDao`, `SalesOrderDao` and `Repository`
  still throw. Align when touching the order DAOs (KAN-101).

## Decisions
- **One bin per receipt.** The user picks one bin; all line items go there.
- **Stock row at 0 is removed.** A `StockItem` means "product P lies in bin X";
  a product has no fixed location, so a row with quantity 0 has no meaning.
  Applies to ship, transfer and adjust.
- **Default picking strategy: largest quantity first** (fewest bins visited).
  `SmallestQuantityFirstStrategy` is the second implementation, to make the
  swap visible. Example: P in A = 8, B = 3, C = 20, need 10 →
  largest-first takes 10 from C; smallest-first takes 3 from B and 7 from A.
- **All-or-nothing** for receive, ship and transfer: stock and order status
  change together or not at all.

## Architecture
- **New concept — Strategy pattern (KAN-100):** `WarehouseService` depends on
  the `PickingStrategy` interface, injected via its constructor; `WarehouseApp`
  chooses the implementation. Strategies are pure logic: no JDBC, no output.
- **New — transaction across DAOs (KAN-101):** today each DAO method runs its
  own transaction. Receive / ship / transfer need one transaction in the
  service spanning `StockItemDao` and an order DAO. Prerequisite: all DAOs
  share one `Connection`. Open point: how the existing DAO-level transactions
  in the order DAOs behave inside a service transaction.
- `COMPLETED` gets its first real use (receive / ship).

## Suggested order
1. **KAN-89, KAN-88** — small domain/service fixes, warm-up
2. **KAN-95** — list master data (useful while testing everything after it)
3. **KAN-94** — clear message on FK violations
4. **KAN-87, KAN-102** — validation (tests already written by /qa as `@Disabled`)
5. **KAN-99** — adjust stock: simplest stock operation, introduces "remove row at 0"
6. **KAN-98** — transfer: two stock rows in one operation
7. **KAN-101** — service-level transaction, applied to transfer first
8. **KAN-96** — receive purchase order
9. **KAN-100** — picking strategy, unit-tested on its own
10. **KAN-97** — ship sales order, combines strategy + transaction
11. Manual test run `Iteration 3 – Acceptance` in Testiny + review

## Concepts Practiced
**New:** Strategy pattern; transactions spanning multiple DAOs.
**Reinforced:** interfaces and polymorphism, constructor injection, enums and
state transitions, JDBC transactions, throw-in-domain / catch-in-UI,
defensive copies (KAN-88), streams/sorting (`Comparator`) inside the strategies.
