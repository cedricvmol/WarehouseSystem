# Testen — commando's & rapporten

## Vereisten

- `JAVA_HOME` wijst naar JDK 21+ (lokaal: `C:\Users\Buurman\.jdks\openjdk-26`).
- Maven hoeft niet geïnstalleerd te zijn: het project bevat de **Maven Wrapper**
  (`mvnw` / `mvnw.cmd`, Maven 3.9.11).

Commando's hieronder zijn voor PowerShell (ook de IntelliJ-terminal).
In Git Bash of Linux: `./mvnw` in plaats van `.\mvnw`.
Zet `-D...`-opties in PowerShell tussen aanhalingstekens.

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
- Allure-indeling: `@Epic` → `@Feature` → `@Story` (tab *Behaviors* in het dashboard).
