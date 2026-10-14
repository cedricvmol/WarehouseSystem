# Testen — commando's & rapporten

## Vereisten

- `JAVA_HOME` wijst naar JDK 21+ (lokaal: `C:\Users\Buurman\.jdks\openjdk-26`).
- Maven hoeft niet geïnstalleerd te zijn: het project bevat de **Maven Wrapper**
  (`mvnw` / `mvnw.cmd`, Maven 3.9.11).

Commando's hieronder zijn voor PowerShell (ook de IntelliJ-terminal).
In Git Bash of Linux: `./mvnw` in plaats van `.\mvnw`.
Zet `-D...`-opties in PowerShell tussen aanhalingstekens.

## Pre-commit hook

Bij elke `git commit` draaien eerst de tests (`.githooks/pre-commit`, ±7 s).
Eén rode test → commit geweigerd. Werkt ook bij committen vanuit IntelliJ.

| Commando | Wat het doet |
|---|---|
| `git config core.hooksPath .githooks` | Hook activeren — eenmalig per clone (lokaal al gedaan) |
| `git commit --no-verify` | Hook overslaan — alleen in noodgevallen |

## Eén klik in IntelliJ

Run-configuratie **Test + Dashboard** (dropdown rechtsboven, naast ▶).
Doet: `clean test allure:serve -Dmaven.test.failure.ignore=true`.
Stoppen met de rode ■ (allure:serve blijft als webserver draaien).

## Basis

| Commando | Wat het doet |
|---|---|
| `.\mvnw clean` | Maakt `target/` leeg (oude builds, resultaten, rapporten) |
| `.\mvnw test` | Draait alle tests + maakt JaCoCo-coverage-rapport |
| `.\mvnw clean test` | Schone lei, dan alle tests |

## Dashboard (Allure)

| Commando | Wat het doet |
|---|---|
| `.\mvnw allure:serve` | Bouwt het dashboard en opent het in je browser (stoppen: Ctrl+C) |
| `.\mvnw allure:report` | Bouwt het dashboard naar `target/site/allure-maven-plugin/` zonder te openen |

Alles in één keer:

```powershell
.\mvnw clean test allure:serve "-Dmaven.test.failure.ignore=true"
```

Open `target/site/allure-maven-plugin/index.html` niet rechtstreeks — dan blijft
het op "loading" hangen. Gebruik `allure:serve`.

## Selectief draaien

| Commando | Wat het doet |
|---|---|
| `.\mvnw test "-Dtest=SalesOrderTest*"` | Eén testklasse, incl. `@Nested`-groepen |
| `.\mvnw test "-Dtest=*OrderTest*"` | Alle klassen die op het patroon passen |
| `.\mvnw test "-Dmaven.test.failure.ignore=true"` | Gaat door bij falende tests, zodat er toch een rapport komt |

## Rapporten als bestand

| Rapport | Locatie |
|---|---|
| Coverage (JaCoCo) | `target/site/jacoco/index.html` |
| Ruwe resultaten (Surefire) | `target/surefire-reports/` |
| Allure-ruwe data | `target/allure-results/` |

## Waar staan de tests

- `src/test/java/com/warehouse/domain/` — unit tests domeinlaag (geen database).
- `src/test/java/com/warehouse/service/` — unit tests `WarehouseService` met Mockito-mocks van de DAO's (geen database).
- Alle unit tests hebben `@Tag("unit")`.
- `src/test/java/com/warehouse/storage/` — integratietests DAO's tegen een echte SQLite-database, `@Tag("integration")`. Elke test krijgt een eigen bestand in `target/test-db/<testnaam>.db` (fixture `TestDatabase`); dat bestand blijft na de run staan, zodat je het met SQL of DBeaver kunt nakijken. Nooit tegen `warehouse.db`.
- Allure-indeling: `@Epic` → `@Feature` → `@Story` (tab *Behaviors* in het dashboard).

## Specificatietests (`@Disabled("KAN-XX")`)

Tests voor werk dat nog niet gebouwd is staan al klaar, maar uitgeschakeld.
Ze verschijnen als **Skipped** in Allure (reden = ticketnummer), blokkeren je
commit niet, en de `@Issue`-link in Allure opent het Jira-ticket.

Werkwijze per ticket: verwijder de `@Disabled` van de tests van dat
ticket/criterium → rood → implementeer → groen → commit.

| Commando | Wat het doet |
|---|---|
| `.\mvnw test "-Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition"` | Draait óók alle uitgeschakelde tests — toont hoeveel werk er nog open staat |

## Integratietests

| Commando | Wat het doet |
|---|---|
| `.\mvnw test "-Dgroups=integration"` | Alleen de integratietests |
| `.\mvnw test "-Dgroups=unit"` | Alleen de unit tests |

Test-DB's openen: `target/test-db/*.db` in DBeaver (driver SQLite).
`.\mvnw clean` gooit ze weg.
