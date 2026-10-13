# Domein-uitleg – WarehouseSystem

Een uitleg in gewone taal van wat elke domeinklasse voorstelt, met een
doorlopend voorbeeld. Bedoeld om op terug te vallen als het conceptueel
even niet meer duidelijk is.

---

## Het grote plaatje in één zin

> Een magazijn **koopt** producten bij leveranciers, **bewaart** ze in vakken,
> en **verkoopt** ze aan klanten.

Alle tien klassen passen in één van deze drie delen:

| Deel | Vraag die het beantwoordt | Klassen |
|---|---|---|
| **1. Stamgegevens** | *Wie* en *wat* bestaat er? | `Supplier`, `Customer`, `Product`, `Bin` |
| **2. Voorraad** | *Waar* ligt *hoeveel*? | `StockItem` |
| **3. Orders** | *Wat* is er *besteld* (in of uit)? | `PurchaseOrder` + `PurchaseOrderLineItem`, `SalesOrder` + `SalesOrderLineItem`, `OrderStatus` |

```
            INKOMEND                  IN HET MAGAZIJN                UITGAAND

  Supplier ──< PurchaseOrder                                SalesOrder >── Customer
                    │                                            │
                    └──< PurchaseOrderLineItem    SalesOrderLineItem >──┘
                              │                         │
                              └──────► Product ◄────────┘
                                          │
                                          └──< StockItem >── Bin

  ──<  betekent "één heeft er veel van"
```

---

## Het voorbeeld: magazijn "FietsOnderdelen BV"

We volgen één magazijn dat fietsonderdelen verdeelt.

### Deel 1 – Stamgegevens: wie en wat bestaat er?

Dit zijn de "vaste" gegevens. Ze veranderen zelden en de rest van het
systeem verwijst ernaar.

**`Supplier` – een leverancier, iemand bij wie wij inkopen**

| supplierId | name | email |
|---|---|---|
| 1 | Shimano Benelux | orders@shimano.be |
| 2 | Continental | sales@conti.de |

**`Customer` – een klant, iemand die bij ons koopt**

| customerId | name | email |
|---|---|---|
| 1 | Fietsen Janssens | info@janssens.be |

**`Product` – een soort artikel dat we verkopen**

| productId | name | threshold | supplier |
|---|---|---|---|
| 10 | Remblok | 50 | Shimano Benelux |
| 20 | Binnenband 28" | 100 | Continental |

- Een `Product` is een **soort** ding, geen fysiek exemplaar. "Remblok" is
  één product, ook al liggen er 300 in het magazijn.
- Een product **zegt niets over hoeveelheid**. Daar is `StockItem` voor.
- `threshold` (drempel) is het minimum dat je in voorraad wilt hebben. Zakt
  de voorraad eronder, dan moet je bijbestellen. Daar doen we pas in
  iteratie 4 iets mee.
- Elk product heeft **één** vaste leverancier (`Supplier`).

**`Bin` – een vak of plaats in het magazijn**

| binId | locationCode |
|---|---|
| 1 | A-01-03 |
| 2 | B-04-01 |

Denk aan een rek met vakjes: `A-01-03` = rek A, plank 1, vak 3. Een bin
weet niet wat erin ligt. Het is gewoon een plaats.

---

### Deel 2 – Voorraad: waar ligt hoeveel?

**`StockItem` – "zoveel stuks van dit product liggen in dit vak"**

| product | bin | quantity |
|---|---|---|
| Remblok | A-01-03 | 200 |
| Remblok | B-04-01 | 100 |
| Binnenband 28" | A-01-03 | 80 |

- Een `StockItem` koppelt een `Product` aan een `Bin`, met een aantal.
- Hetzelfde product kan in **meerdere vakken** liggen (remblokken liggen
  in A-01-03 én B-04-01).
- In één vak kunnen **meerdere producten** liggen.
- `checkStockLevel(10)` telt alle `StockItem`s van product 10 op:
  200 + 100 = **300 remblokken** in totaal.

> **Dit is de enige plek in het systeem die zegt hoeveel er fysiek ligt.**

---

### Deel 3 – Orders: wat is er besteld?

Een order is een **document**, een afspraak. Denk aan een bestelbon op
papier. Een order is nog **geen** fysieke beweging van goederen.

Er zijn twee richtingen:

| | `PurchaseOrder` (inkooporder) | `SalesOrder` (verkooporder) |
|---|---|---|
| Richting | **Binnen**komend | **Buiten**gaand |
| Wie | Wij bestellen bij een **`Supplier`** | Een **`Customer`** bestelt bij ons |
| Gevolg later | Voorraad gaat **omhoog** | Voorraad gaat **omlaag** |

#### Waarom heeft een order "line items"?

Kijk naar een kassaticket of factuur: bovenaan staan de gegevens van de hele
bon (wie, wanneer), daaronder volgen de **regels**, één per artikel.

```
┌─────────────────────────────────────────────┐
│ INKOOPORDER #7                              │  ← PurchaseOrder
│ Leverancier: Shimano Benelux                │     (supplier)
│ Datum: 13/10/2026 14:30                     │     (orderDate)
│ Status: OPEN                                │     (orderStatus)
├─────────────────────────────────────────────┤
│ Remblok ................... 500 stuks       │  ← PurchaseOrderLineItem
│ Remkabel .................. 200 stuks       │  ← PurchaseOrderLineItem
└─────────────────────────────────────────────┘
```

- **`PurchaseOrder`** = de **kop** van de bon: welke leverancier, wanneer,
  welke status, en de lijst met regels.
- **`PurchaseOrderLineItem`** = **één regel** op de bon: welk product en
  hoeveel. Een line item bestaat nooit alleen. Het hoort altijd bij een order.

Een verkooporder werkt precies hetzelfde:

```
┌─────────────────────────────────────────────┐
│ VERKOOPORDER #3                             │  ← SalesOrder
│ Klant: Fietsen Janssens                     │     (customer)
│ Datum: 13/10/2026 15:00                     │
│ Status: OPEN                                │
├─────────────────────────────────────────────┤
│ Remblok ................... 40 stuks        │  ← SalesOrderLineItem
│ Binnenband 28" ............ 25 stuks        │  ← SalesOrderLineItem
└─────────────────────────────────────────────┘
```

#### `OrderStatus` – in welke fase zit de order?

```
            cancel()
   OPEN  ────────────►  CANCELLED
     │
     │  (iteratie 3: ontvangen / verzenden)
     ▼
  COMPLETED
```

- **OPEN**: aangemaakt, nog niet afgehandeld. Elke nieuwe order start hier.
- **CANCELLED**: geannuleerd. Kan alleen vanuit OPEN.
- **COMPLETED**: afgehandeld (goederen ontvangen of verzonden). Ook dan kan
  je niet meer annuleren.

Daarom gooit `cancel()` een `IllegalStateException` als de status niet OPEN
is: de order zit in de verkeerde *toestand* (state) om te annuleren.

---

## De meest verwarrende punten op een rij

### 1. `StockItem` versus `LineItem`, beide hebben product + aantal?

Ze lijken op elkaar, maar betekenen iets totaal anders:

| | `StockItem` | `PurchaseOrderLineItem` / `SalesOrderLineItem` |
|---|---|---|
| Betekenis | "Er **ligt** X stuks in vak Y" | "Er is X stuks **besteld**" |
| Is het echt? | Ja, fysieke werkelijkheid | Nee, een afspraak op papier |
| Heeft een `Bin`? | Ja | Nee, een bestelling weet niet in welk vak iets komt |
| Hoort bij | Het magazijn | Eén order |

### 2. Verandert het aanmaken van een order de voorraad?

**Nee, nog niet.** Dat is bewust zo. In iteratie 2 maak je enkel het
**document** aan. Het effect op de voorraad komt in **iteratie 3**
(Fulfillment & Stock Movement):

- Inkooporder **ontvangen** → `StockItem`s gaan omhoog, status → COMPLETED
- Verkooporder **verzenden** → `StockItem`s gaan omlaag, status → COMPLETED

Nu bestellen bij Shimano verandert de 300 remblokken dus niet. Pas als de
levering binnenkomt (iteratie 3) worden het er 800.

### 3. `Product` heeft een `Supplier`, en `PurchaseOrder` ook?

- `Product.supplier` = de **vaste** leverancier van dat artikel (stamgegeven).
- `PurchaseOrder.supplier` = bij **wie deze specifieke bestelling** geplaatst is.

In de praktijk zijn die meestal dezelfde, maar het zijn twee verschillende
feiten.

### 4. Waarom heeft een order een eigen ID die de database kiest?

Bij `Product`, `Bin` en de andere stamgegevens typ jij zelf de ID in. Bij
orders genereert SQLite de ID (`AUTOINCREMENT`). Een ordernummer is een
"zakelijk" nummer dat nooit opnieuw gebruikt mag worden, ook niet na een
delete. Daarom krijgt een nieuwe order in Java eerst `0` als placeholder, en
geeft de DAO na de insert het echte nummer terug.

---

## Het hele verhaal als tijdlijn

| # | Wat gebeurt er in het echt | In het systeem | Iteratie |
|---|---|---|---|
| 1 | We leggen Shimano vast als leverancier | `addSupplier` → `Supplier` | 1 ✅ |
| 2 | We beslissen remblokken te verkopen | `addProduct` → `Product` | 1 ✅ |
| 3 | We nummeren de vakken in het rek | `addBin` → `Bin` | 1 ✅ |
| 4 | Beginvoorraad: 200 remblokken in A-01-03 | `addStockItem` → `StockItem` | 1 ✅ |
| 5 | We bestellen 500 remblokken bij Shimano | `createPurchaseOrder` → `PurchaseOrder` (OPEN) | 2 🔨 |
| 6 | Janssens bestelt 40 remblokken | `createSalesOrder` → `SalesOrder` (OPEN) | 2 🔨 |
| 7 | Janssens annuleert toch | `cancelSalesOrder` → status CANCELLED | 2 🔨 |
| 8 | Levering van Shimano komt binnen | ontvangen → `StockItem` +500, COMPLETED | 3 |
| 9 | We verzenden een order naar een klant | verzenden → `StockItem` −X, COMPLETED | 3 |
| 10 | Voorraad remblokken zakt onder 50 | melding "bijbestellen!" | 4 |

---

## Geheugensteuntje

- **Stamgegevens** = de *zelfstandige naamwoorden* van het magazijn (wie, wat, waar)
- **StockItem** = de *werkelijkheid* (wat ligt er nu)
- **Orders** = de *afspraken* (wat gaat er binnenkomen of buitengaan)
- **Line item** = één *regel* op een afspraak
- **OrderStatus** = waar die afspraak staat in haar *levensloop*
