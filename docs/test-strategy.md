# Teststrategie — WarehouseSystem

> Het *waarom en wat* van het testen. Voor de *hoe* (commando's, rapporten)
> zie `testing.md`.
>
> Status: **in opbouw**. Wat onder "Beslist" staat ligt vast; de rest is een
> voorstel tot het in de beslissingenlijst onderaan staat.

## Doel

- Elke merge draait automatisch door een pipeline met unit-, integratie- en
  E2E-tests.
- Manuele tests (Testiny) vullen aan waar automatisatie niet loont.
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
| **Unit** | Domeinregels (statussen, validatie, berekeningen), service-logica met fakes | Geen | JUnit 5 | Elke build |
| **Integratie** | DAO's en services samen met een echte database | Eigen SQLite-bestand per test | JUnit 5 + SQL-asserts | Elke build |
| **E2E** | Volledige app via de CLI, als gebruiker | Eigen test-DB per scenario | JUnit 5, app als proces (stdin/stdout) | Elke merge |
| **Manueel** | Exploratief, gebruiksvriendelijkheid, wat niet automatiseerbaar loont | Test-DB, te openen in DBeaver | Testiny | Per iteratie |

Verdeling: veel unit, minder integratie, weinig maar waardevolle E2E
(testpiramide).

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

Playwright/Selenium sturen een browser aan; deze app heeft een CLI. E2E is
hetzelfde idee met een ander "scherm":

1. Start de app als proces (`java -jar ...`) met een test-DB.
2. Stuur invoer naar stdin (menukeuzes, waarden).
3. Controleer de console-output.
4. Controleer de eindtoestand met SQL op de test-DB.

Scenario's komen rechtstreeks uit de Given/When/Then van de stories.
Komt er later een web-UI, dan wordt de driver Playwright for Java — de laag
blijft dezelfde.

### Manueel

- Testcases en runs in Testiny, gelinkt aan KAN-stories.
- Uitvoeren tegen een aparte test-DB, nooit tegen `warehouse.db`.

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
3. E2E-tests
4. Allure- en JaCoCo-rapport publiceren

Uitvoering is altijd deterministisch via Maven — geen interpretatie in
pass/fail.

## Agents per laag (idee)

Agents **schrijven, ontwerpen en analyseren** tests; ze **voeren ze niet
uit** (dat doet Maven/CI).

- Unit-agent — domeinregels, grenswaarden
- Integratie-agent — DAO/service met SQL-asserts
- E2E-agent — CLI-scenario's uit Given/When/Then
- Triage-agent — bij rode pipeline: bug, testfout of flaky?

Voorstel: pas een agent maken wanneer een laag bestaat en herhaalwerk heeft.

## Open beslissingen

- [x] Hoe wordt de DB-URL doorgegeven (constructor-parameter, systeemproperty, config-bestand)? → zie Beslist 2026-10-14
- [ ] E2E in JUnit (Java) of black-box in een andere taal (bv. pytest)?
- [ ] Structuur in Testiny (mappen, naamgeving, link naar KAN)
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
