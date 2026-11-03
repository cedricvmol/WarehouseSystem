-- =====================================================================
-- Seed data for MANUAL tests (Testiny) against warehouse.db
-- =====================================================================
-- Usage: open warehouse.db in DBeaver, open this file in a SQL editor
--        and run it as a script (Alt+X). Start the app once first, so
--        the tables exist.
--
-- Safe to re-run: INSERT OR IGNORE skips rows whose ID already exists,
-- existing data is never changed or deleted.
--
-- Covers test run TR-1 "Iteration 2 – Acceptance" (TC-1 … TC-18) and
-- TC-19 / TC-20 (KAN-87) and the KAN-95 list cases. Add new base data here when new test cases
-- need it.
-- =====================================================================

-- Suppliers
INSERT OR IGNORE INTO suppliers (supplierId, name, email) VALUES
    (1, 'Acme Supplies', 'orders@acme.test');

-- Customers
INSERT OR IGNORE INTO customers (customerId, name, email) VALUES
    (1, 'Globex Retail', 'purchasing@globex.test');

-- Products (all from supplier 1)
INSERT OR IGNORE INTO products (productId, name, threshold, supplierId) VALUES
    (101, 'Pallet wrap film', 10, 1),
    (102, 'Cardboard box L',  20, 1),
    (103, 'Packing tape',     15, 1);

-- Bins
INSERT OR IGNORE INTO bins (binId, locationCode) VALUES
    (1, 'A-01-01'),
    (2, 'A-01-02');


-- =====================================================================
-- Checks — run after the inserts. Every row should say OK.
-- =====================================================================
-- If a check says NOT OK: the ID already existed in this database with
-- different data (INSERT OR IGNORE kept the old row). Fix it by hand or
-- adjust the test case.

SELECT 'supplier 1 exists'              AS "check", CASE WHEN EXISTS (SELECT 1 FROM suppliers WHERE supplierId = 1)  THEN 'OK' ELSE 'NOT OK' END AS result
UNION ALL
SELECT 'customer 1 exists',                          CASE WHEN EXISTS (SELECT 1 FROM customers WHERE customerId = 1)  THEN 'OK' ELSE 'NOT OK' END
UNION ALL
SELECT 'products 101-103 of supplier 1',             CASE WHEN (SELECT COUNT(*) FROM products WHERE productId IN (101, 102, 103) AND supplierId = 1) = 3 THEN 'OK' ELSE 'NOT OK' END
UNION ALL
SELECT 'bins 1-2 with location codes',               CASE WHEN (SELECT COUNT(*) FROM bins WHERE (binId = 1 AND locationCode = 'A-01-01') OR (binId = 2 AND locationCode = 'A-01-02')) = 2 THEN 'OK' ELSE 'NOT OK' END
UNION ALL
SELECT 'supplier 999 does not exist',               CASE WHEN NOT EXISTS (SELECT 1 FROM suppliers WHERE supplierId = 999) THEN 'OK' ELSE 'NOT OK' END
UNION ALL
SELECT 'customer 999 does not exist',                CASE WHEN NOT EXISTS (SELECT 1 FROM customers WHERE customerId = 999) THEN 'OK' ELSE 'NOT OK' END
UNION ALL
SELECT 'product 999 does not exist',                 CASE WHEN NOT EXISTS (SELECT 1 FROM products WHERE productId = 999)   THEN 'OK' ELSE 'NOT OK' END
UNION ALL
SELECT 'purchase order 999 does not exist',          CASE WHEN NOT EXISTS (SELECT 1 FROM purchaseOrders WHERE purchaseOrderId = 999) THEN 'OK' ELSE 'NOT OK' END
UNION ALL
SELECT 'sales order 999 does not exist',             CASE WHEN NOT EXISTS (SELECT 1 FROM salesOrders WHERE salesOrderId = 999)       THEN 'OK' ELSE 'NOT OK' END;
