# Teststrategie — WarehouseSystem

> Het *waarom en wat* van het testen. Voor de *hoe* (commando's, rapporten)
> zie `testing.md`.
>
> Status: **in opbouw**. Wat onder "Beslist" staat ligt vast; de rest is een
> voorstel tot het in de beslissingenlijst onderaan staat.

## Doel

- Elke merge draait automatisch door een pipeline met unit- en
  integratietests (incl. UI-tests met gemockte service).
- End-to-end testen gebeurt **manueel** (Testiny) zolang de app een CLI is.
  Komt er een web-UI, dan worden E2E-tests geautomatiseerd met Playwright.
- Resultaten zijn controleerbaar met SQL op de database, zoals in de praktijk.
- Elke acceptatiecriterium van een story (KAN-XX) is traceerbaar naar minstens
  één test.

## Testbasis

Vanaf iteratie 3 is elke Story een use case met Given/When/Then-
acceptatiecriteria. Die criteria zijn de testbasis:

```
Story (KAN-XX) → acceptatiecriterium → testcase (automatisch of manueel)
```

In code via Allure: `@Epic` / `@Feature` / `@Story` en `@Link("KAN-XX")`.

## Testlagen

De app is gelaagd: `domain` → `service` (`WarehouseService`) → `storage`
(DAO's, SQLite via JDBC) → `ui` (CLI, `WarehouseUI`). De testlagen volgen dat.

| Laag | Wat wordt getest | Database | Tool | Draait |
|---|---|---|---|---|
| **Unit** | Domeinregels (statussen, validatie, berekeningen), service-logica met mocks, CLI-bedrading met gemockte service (invoer, menu-routering, meldingen) | Geen | JUnit 5 + Mockito | Elke build |
| **Integratie** | DAO's en services samen met een echte database | Eigen SQLite-bestand per test | JUnit 5 + SQL-asserts | Elke build |
| **E2E (manueel)** | Volledige app via de CLI, als gebruiker: acceptatie per iteratie, regressie (Critical-cases), exploratief | `warehouse.db` met seed, te openen in DBeaver | Testiny | Per iteratie |
| **E2E (automatisch)** | *Uitgesteld* — pas bij een web-UI | Eigen test-DB per scenario | Playwright for Java | Elke merge |

Verdeling: veel unit, minder integratie, end-to-end manueel (testpiramide;
de top wordt pas geautomatiseerd als er een web-UI is).

### Unit

- Snel, geen I/O, geen database.
- Focus: grenswaarden, negatieve paden, statusovergangen (bv. order
  OPEN → CANCELLED, CANCELLED → CANCELLED geweigerd).
- Locatie: `src/test/java/com/warehouse/domain/`, later `.../service/`.

### Integratie

Zelfde concept als pytest met fixtures:

| pytest | JUnit 5 |
|---|---|
| `@pytest.fixture` | `@BeforeEach` / `@AfterEach` of een Extension |
| `tmp_path` | `@TempDir` |
| fixture `scope="session"` | `@BeforeAll` |
| `@pytest.mark.parametrize` | `@ParameterizedTest` |
| `conftest.py` | Gedeelde basisklasse / Extension |
| markers (`-m integration`) | `@Tag("integration")` |

- Elke test: lege DB → schema via `initTables()` → eigen testdata → actie →
  assert op returnwaarde **en** via SQL op de tabellen.
- Negatieve cases op DB-niveau (bv. order met onbestaande klant → geweigerd).
- Locatie: `src/test/java/com/warehouse/storage/`, `@Tag("integration")`.
  Fixture `TestDatabase`: per test een vers bestand in `target/test-db/`
  (blijft staan voor inspectie met SQL/DBeaver).

### E2E

**Zolang de app een CLI is: manueel.** E2E-scenario's zijn de manuele
testcases in Testiny (zie Manueel), afgeleid van de Given/When/Then van de
stories en gecontroleerd met SQL op `warehouse.db`.

Waarom geen automatische CLI-E2E (app als proces, stdin/stdout, SQL)? Dat
zou exact hetzelfde doen als de manuele run. De automatische UI-tests
dekken de CLI-bedrading al (met gemockte service), de onderste lagen
vangen de meeste regressies op; manueel blijft wat end-to-end waarde
heeft.

**Komt er een web-UI:** dan worden E2E-tests geautomatiseerd met
**Playwright for Java**, tegen een eigen test-DB per scenario
(systeemproperty `-Dwarehouse.db.url=...`, zie Beslist 2026-10-14), en
draaien ze in de pipeline. Scenario's blijven uit de Given/When/Then van de
stories komen.

### Manueel

- Testcases en runs in Testiny (project `P-1` WareHouseProject), gelinkt
  aan KAN-stories. Testiny is gekoppeld met Jira: gelinkte testcases en
  runs zijn zichtbaar op de Jira-issue.
- Uitvoeren tegen `warehouse.db`, met de data die er al in zit. Geen
  nieuwe DB per run: bestaande data is realistischer en vaak handig voor
  exploratief testen. Resultaten nakijken met SQL (DBeaver).
- Automatische tests (unit, integratie, later Playwright-E2E) gebruiken wél altijd een eigen
  DB en raken `warehouse.db` nooit.
- `warehouse.db` staat niet in git (`*.db` in `.gitignore`): elke machine
  heeft een eigen DB. Gedeelde basisdata komt uit
  `src/test/resources/manual-test-data.sql` (seed, `INSERT OR IGNORE`,
  veilig herhaalbaar, met controlequeries onderaan). Testcases steunen
  alleen op de seed en op wat ze zelf aanmaken.
- **Herhaalbaar zonder opruimen.** Een case moet elke run opnieuw kunnen
  draaien zonder `DELETE` vooraf:
  - Vrij ID bepalen in de eerste stap:
    `SELECT COALESCE(MAX(productId), 0) + 1 FROM products;` → `<newId>`.
  - Duplicaat testen = eerst zelf aanmaken met `<newId>`, daarna opnieuw
    met hetzelfde ID.
  - Afhankelijke data (bv. stock) op een record dat de case zelf maakt,
    zodat er gegarandeerd nog niets op staat.
  - Seed alleen voor stabiele referentiedata (supplier 1, customer 1,
    bins 1/2, "999 bestaat niet").
  - De DB groeit bij elke run met een paar rijen; dat is aanvaard.
- **Wat manueel, wat automatisch.** Validatieregels (grenswaarden,
  ongeldige invoer) worden gedekt door unit-tests — geen manuele case,
  tenzij de CLI er iets eigens mee doet. Manueel test flows end-to-end en
  wat de gebruiker ziet (bv. order aanmaken, stock toevoegen en niveau
  checken). Regels die via de CLI onbereikbaar zijn (bv. order zonder
  partij) zijn sowieso alleen automatisch.

#### Structuur in Testiny

Mappen volgen het **domein**, niet de iteratie (functionaliteit blijft,
iteraties passeren). Max. 2 niveaus; een map pas aanmaken als er cases in
komen.

```
01 Master data          → Products, Suppliers, Customers, Bins
02 Orders               → Purchase orders, Sales orders
03 Fulfillment & stock
04 Alerts & reporting
90 Cross-cutting        → Input validation, CLI navigation, Persistence
```

Testcases:
- Titel `[KAN-XX] <gedrag in gewone taal>`, bv.
  `[KAN-85] Create purchase order with one line item`.
- Minstens één case per acceptatiecriterium; AC-nummer in de precondition
  ("Covers KAN-85 AC2").
- Template STEPS: Given → precondition, When → stappen, Then → verwacht
  resultaat.
- `testcase_type`: Acceptance (uit AC), Usability (exploratief), Regression
  (set die elke iteratie opnieuw draait).
- `priority`: Critical = happy path kernflow, Medium = negatieve paden,
  Low = cosmetisch.
- Jira-link: de student zet die zelf in de Testiny-UI (de connector kan
  geen links leggen). De key in de titel is aanvullend.

Testruns:
- Per iteratie: `Iteration N – Acceptance`, gelinkt aan de testtaak in Jira.
- Later: `Regression – <datum/versie>` met de Regression-cases.
- Later te bekijken: JUnit-XML importeren in Testiny zodat ook
  automatische resultaten in Testiny/Jira verschijnen.

## Database-afspraken (van bij de start)

Doel: later niet vastlopen bij CI, isolatie of een andere database.

1. **Configureerbare DB-URL.** Productie gebruikt `warehouse.db`; elke test
   zijn eigen bestand. *Vereiste:* `DatabaseManager` krijgt een URL van
   buitenaf (productiecode → taak van de student). **Blocker** voor
   integratie- en E2E-tests.
2. **Schema op één plek** (`DatabaseManager.initTables()`). Later eventueel
   migraties (Flyway) zodat test en productie gegarandeerd hetzelfde schema
   hebben.
3. **Foreign keys aan.** SQLite controleert `FOREIGN KEY` standaard niet;
   per connectie `PRAGMA foreign_keys = ON`. Anders test je tegen een DB die
   minder streng is dan je denkt.
4. **Geïsoleerde tests.** Elke test start leeg en maakt zijn eigen data;
   geen volgorde-afhankelijkheid. Voorwaarde voor CI.
5. **Inspecteerbaar.** Test-DB's blijven na de run staan (bv.
   `target/test-db/`) zodat je met SQL/DBeaver kunt nakijken wat er gebeurde.
6. **Standaard-SQL.** Houdt de overstap naar PostgreSQL + Testcontainers open.

Nooit: tests tegen de echte `warehouse.db` in de projectroot.

## Rapportering

- **Allure** — dashboard, indeling Epic → Feature → Story, later links naar
  KAN-tickets.
- **JaCoCo** — code coverage (`target/site/jacoco/index.html`).
- In CI: rapporten als artefact van de pipeline-run.

## Pipeline (doel)

Bij elke merge (GitHub Actions):

1. Build + unit-tests
2. Integratietests
3. Allure- en JaCoCo-rapport publiceren

Bij een web-UI komt er een stap bij: E2E-tests met Playwright (na de
integratietests).

Uitvoering is altijd deterministisch via Maven — geen interpretatie in
pass/fail.

## Agents per laag (idee)

Agents **schrijven, ontwerpen en analyseren** tests; ze **voeren ze niet
uit** (dat doet Maven/CI).

- Unit-agent — domeinregels, grenswaarden
- Integratie-agent — DAO/service met SQL-asserts
- E2E-agent — Playwright-scenario's uit Given/When/Then (pas bij een web-UI)
- Triage-agent — bij rode pipeline: bug, testfout of flaky?

Voorstel: pas een agent maken wanneer een laag bestaat en herhaalwerk heeft.

## Open beslissingen

- [x] Hoe wordt de DB-URL doorgegeven (constructor-parameter, systeemproperty, config-bestand)? → zie Beslist 2026-10-14
- [x] E2E in JUnit (Java) of black-box in een andere taal (bv. pytest)? → zie Beslist 2026-11-02 (manueel zolang CLI; Playwright bij web-UI)
- [x] Structuur in Testiny (mappen, naamgeving, link naar KAN) → zie Beslist 2026-11-02
- [ ] Agents per laag: ja/nee, en wanneer
- [ ] BDD (Cucumber/Gherkin) voor E2E-scenario's?
- [ ] Bugs uit testen loggen als Jira-issue: werkwijze
- [ ] SQLite blijven of later PostgreSQL + Testcontainers

## Beslist

- 2026-10-13 — JUnit 5 voor geautomatiseerde tests; Allure + JaCoCo voor
  rapportering; Maven Wrapper met JDK 21+.
- 2026-10-13 — QA start vanaf iteratie 3; testcases afgeleid van
  Given/When/Then-acceptatiecriteria (traceerbaarheid story → criterium →
  test).
- 2026-10-13 — Doel: CI-pipeline bij elke merge; verificatie met SQL op de
  database.
- 2026-10-13 — Unit-tests draaien automatisch via een Git pre-commit hook
  (commit geweigerd bij rode test). CI komt later als vangnet.
- 2026-10-13 — Service-unit-tests gebruiken Mockito (mocks van de concrete
  DAO-klassen). DAO-interfaces zijn een designkeuze, geen testvereiste; de
  tests blijven werken als die er later komen.
- 2026-10-13 — Pass rate: 100% verplicht (één rode test = commit
  geweigerd). Code coverage: alleen rapporteren (JaCoCo/Allure), geen harde
  gate. Een gate op `domain` + `service` kan later, als die volledig getest
  zijn.
- 2026-10-13 — Nieuwe regels en bugfixes: TDD-stijl. /qa schrijft eerst de
  (rode) specificatietests met `@Disabled("KAN-XX")` en `@Issue("KAN-XX")`;
  de student verwijdert `@Disabled` en implementeert tot groen. Grenswaarden
  die *toegelaten* moeten zijn, draaien meteen mee.
- 2026-10-14 — DB-URL via constructor: `DatabaseManager(String url)` voor
  tests; `DatabaseManager()` blijft `warehouse.db` gebruiken (productie
  ongewijzigd). Voor E2E later een systeemproperty (bv.
  `-Dwarehouse.db.url=...`) in `WarehouseApp` bovenop die constructor.
  Productiewerk: KAN-90 (URL) en KAN-91 (`PRAGMA foreign_keys = ON`);
  integratietests staan klaar als `@Disabled`-specs.
- 2026-11-02 — Structuur in Testiny: mappen per domein (niet per
  iteratie), titel `[KAN-XX] ...`, minstens één case per AC, STEPS-template,
  runs per iteratie. Jira-links zet de student zelf in de UI; key in de
  titel is aanvullend. Details: sectie Manueel → Structuur in Testiny.
- 2026-11-02 — Uitzondering iteratie 2 (oude structuur met subtaken): alle
  order-testcases (TC-1 … TC-18) hangen aan subtaak KAN-86; de
  validatiecases TC-19 en TC-20 aan story KAN-87. Vanaf iteratie 3
  hangen testcases aan de user story waarvan ze een AC dekken.
- 2026-11-02 — Manuele tests draaien tegen `warehouse.db` met bestaande
  data, geen aparte DB per run (realistischer, handig voor exploratief
  testen). Automatische tests blijven een eigen DB gebruiken.
- 2026-11-02 — E2E blijft **manueel** (Testiny) zolang de app een CLI is;
  geen automatische CLI-E2E (zou de manuele run verdubbelen). UI-tests met
  gemockte service blijven automatisch (bedrading, invoer, meldingen).
  Komt er een web-UI, dan E2E automatisch met Playwright for Java in de
  pipeline.
- 2026-11-03 — Lege-toestand-scenario's (bv. "lijst is leeg → melding",
  KAN-95 AC2/AC6) worden **alleen automatisch** gedekt (UI-test met
  gemockte service die een lege lijst teruggeeft). Geen manuele case: die
  zou een lege `warehouse.db` vereisen, wat botst met "manueel tegen
  bestaande data".
- 2026-11-03 — Manuele cases zijn **herhaalbaar zonder opruimen**: vrije
  ID's via `MAX + 1` (`<newId>`), duplicaten door eerst zelf aan te maken,
  afhankelijke data op een zelf gemaakt record; seed alleen voor stabiele
  referentiedata. Reden: een `DELETE` vóór elke run kost telkens tijd.
  Toegepast op TC-25 … TC-34; dit is de basis voor de regressieset.
- 2026-11-04 — **Validatieregels alleen automatisch.** Grenswaarden en
  ongeldige invoer zijn gedekt door unit-tests; manuele cases zijn voor
  flows en wat de gebruiker ziet. Reden: een manuele case voor bv.
  "stock -1 geweigerd" vindt niets wat de unit-test niet al vindt en maakt
  het testwerk onnodig zwaar. Toegepast op KAN-87 (geen nieuwe manuele
  cases).
