# FlyRes

FlyRes is a desktop flight reservation system built for the ENSF 614 term project at the University of Calgary. It is a single-user app with three roles (customer, flight agent and administrator) and runs on Java 21, JavaFX 21 and an embedded SQLite database.

This README is the developer guide. It covers how to run, develop, build and test the system.

---

## Prerequisites

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | 17 or newer, to start Gradle | Gradle downloads **JDK 21** automatically on the first build (Foojay toolchain resolver). You don't need to install it. |
| Git | any recent version | |
| Gradle | **don't install it** | Always use the wrapper (`./gradlew`) that is checked into the repo. |

You don't need to install JavaFX or SQLite either. Gradle fetches both.

> **Windows:** use `.\gradlew.bat` in PowerShell or `cmd`, or `./gradlew` in Git Bash. The examples below use `./gradlew`.

---

## Quick start

```bash
git clone <repo-url> FlyRes
cd FlyRes
./gradlew run
```

The first run downloads Gradle, JDK 21 and the dependencies, so it takes a few minutes. Later runs are fast.

On first launch the app creates its database at `~/.flyres/flyres.db` (on Windows, `C:\Users\<you>\.flyres\flyres.db`). It builds the database from `schema.sql` and fills it from `seed.sql`.

---

## Commands

| Command | What it does |
| --- | --- |
| `./gradlew build` | Compiles everything and runs all tests, including `ArchitectureTest`. **It must pass before you open a PR.** |
| `./gradlew run` | Launches the app. |
| `./gradlew test` | Runs all tests. |
| `./gradlew :backend:test --tests "com.flyres.data.DatabaseManagerTest"` | Runs one test class. |
| `./gradlew :frontend:shadowJar` | Builds a runnable fat JAR at `frontend/build/libs/FlyRes.jar`. Run it with `java -jar` on JDK 21. |
| `./gradlew packageSource` | Zips every `.java` file into `build/distributions/FlyRes-source.zip` (a submission requirement). |
| `./gradlew clean` | Deletes all build output. |

### Resetting the database

There are no migrations. After any change to `schema.sql` or `seed.sql`, delete the database. The next launch recreates and reseeds it.

```bash
rm -rf ~/.flyres                            # macOS / Linux / Git Bash
Remove-Item -Recurse -Force $HOME\.flyres   # PowerShell
```

---

## Project structure

The project has two Gradle modules. The backend has **no JavaFX dependency**.

```
backend/                       business and data layers
  com.flyres.FlyResBackend     facade: wires repositories into controls; the UI's only entry point
  com.flyres.domain.*          <<entity>>   user/, flight/, booking/, promotion/
  com.flyres.control.*         <<control>>  one *Controller per major use case; strategy/ for patterns
  com.flyres.repository.*      repository interfaces only
  com.flyres.data              DatabaseManager (Singleton), TransactionRunner
  com.flyres.data.sqlite.*     Sqlite*Repository implementations, the ONLY place SQL lives
  com.flyres.common.*          exception/, security/, validation/
  src/main/resources/db/       schema.sql, seed.sql
frontend/                      presentation layer (JavaFX)
  com.flyres.ui                Launcher (main), FlyResApp (Application)
  com.flyres.ui.navigation     SceneRouter, ViewId
  com.flyres.ui.component      reusable JavaFX components
  com.flyres.ui.view.*         <<boundary>> *ViewController classes, grouped by feature
  src/main/resources/com/flyres/ui/
    fxml/                      one FXML file per screen, mirroring ui.view.*
    css/flyres.css             app styles on top of AtlantaFX Primer Light
docs/diagrams/                 PlantUML sources for the design document's UML diagrams
gradle/libs.versions.toml      every dependency version, in one place
```

---

## Architecture rules

The project is graded on design quality and traceability as much as on features. `ArchitectureTest` (in `frontend/src/test`) enforces rules 1 to 5, so a failing build means the design is wrong. **Fix the code, not the test.**

1. `frontend` uses only `FlyResBackend`, `control`, `domain` and `common.exception`. It never touches `repository`, `data` or JDBC.
2. `backend` never depends on JavaFX.
3. `control` depends on `repository` **interfaces**, never on `data.sqlite` classes. `FlyResBackend` injects the implementations through constructors.
4. `domain` depends only on `domain` and `common`.
5. SQL lives only in `data.sqlite` and `resources/db/*.sql`.
6. `*ViewController` classes contain no business rules. A view controller reads input, calls one control method, then shows the result or the error.
7. Role checks happen in the control. Each restricted control method takes the acting `User` and throws `AccessDeniedException` when the role is wrong. Hiding a button is only cosmetic.

### Naming

| Kind | Stereotype | Package | Example |
| --- | --- | --- | --- |
| Domain object | `<<entity>>` | `domain.*` | `Flight`, `Reservation` |
| Use-case coordinator | `<<control>>` | `control` | `BookingController` |
| JavaFX FXML controller | `<<boundary>>` | `ui.view.*` | `FlightSearchViewController` |
| Data access | none | `repository` / `data.sqlite` | `FlightRepository` / `SqliteFlightRepository` |

A JavaFX class is never named `*Controller` without `View`. `ArchitectureTest` checks this too.

The set of control classes is fixed: `AuthenticationController`, `FlightSearchController`, `BookingController`, `PaymentController`, `FlightManagementController`, `CustomerManagementController` and `PromotionController`. Add methods to these rather than creating new controls, because the sequence diagrams depend on them.

---

## Developing a feature

Work happens on issues `FLY-01` to `FLY-26`.

1. Read the issue and its acceptance criteria. Check that its dependencies are merged.
2. Branch from `master`: `feature/FLY-<nn>-short-name`.
3. Build from the bottom up:
   `schema.sql` → `domain` → `repository` interface → `data.sqlite` implementation → `control` (with role checks) → wire it in `FlyResBackend` → FXML + `*ViewController`.
4. Write tests alongside the control code (see [Testing](#testing)).
5. Run `./gradlew build`. Then reset the database and run `./gradlew run`, and walk through every acceptance criterion by hand.
6. If you added a class or a public control method, update `docs/diagrams/class-diagram.puml`.
7. Commit messages follow the form `FLY-<nn>: <imperative summary>`, for example `FLY-14: book flight in a single transaction`.
8. Open a PR with `Closes #<issue>` and the acceptance checklist ticked.

### Coding conventions

- **Java 21.** Use `record` for immutable value objects and regular classes for entities with identity and state.
- **No frameworks:** no Spring, no ORM, no Lombok, no DI container. Wire objects with plain constructors in `FlyResBackend`.
- **Money** is a `long` number of cents everywhere, including the DB (`INTEGER`). Format it only in the UI.
- **Time** uses `java.time` only and is stored as ISO-8601 `TEXT`. Inject a `Clock` (and a `Random`) instead of calling `LocalDateTime.now()` or `new Random()` in a control.
- **SQL** always uses `PreparedStatement` with `?` parameters. Never concatenate SQL strings. Every multi-step write runs in one transaction.
- **Exceptions:** controls throw `ValidationException`, `NotFoundException`, `AccessDeniedException` or `BookingException`. View controllers catch them and show them inline or as a toast. Never swallow an exception.
- **Security:** never store or log plain passwords, full card numbers or CVVs.
- **Dependencies:** ask the team before adding one. The approved set is JavaFX, AtlantaFX, sqlite-jdbc, JUnit 5, ArchUnit and the Shadow plugin. Versions live in `gradle/libs.versions.toml`.

### UI conventions

- One FXML file per screen under `fxml/<feature>/`. All navigation goes through `SceneRouter` inside a single window.
- **No inline styles** (`style="..."` or `setStyle`). Use CSS classes from `flyres.css` and the AtlantaFX theme classes such as `title-1` and `text-muted`.
- Keep spacing on an 8 px scale and use cards for grouped content.
- Show validation errors inline under the field and success as a toast.
- Every destructive action (cancel, archive) asks for confirmation and says what will happen.

---

## Testing

- Tests use **JUnit 5** and run with `./gradlew test`. HTML reports are written to `<module>/build/reports/tests/test/index.html`.
- Backend tests get a fresh in-memory database per test from `DatabaseManager.forTesting()`. They never touch `~/.flyres`.
- Every new control method needs at least one success test and one failure test (wrong role, invalid input or a broken business rule).
- `Reservation` and `Payment` state transitions are tested, including illegal ones.
- Test names follow `methodName_condition_expectedResult`, for example `book_seatAlreadyTaken_throwsBookingException`.
- The UI is verified by hand against each issue's acceptance criteria. There are no automated UI tests.

---

## Demo accounts

The seed data creates these accounts. They are demo values for a local course project.

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@flyres.ca` | `Admin#2026` |
| Agent | `agent@flyres.ca` | `Agent#2026` |
| Customer | `maya@example.com` | `Customer#2026` |
| Customer | `omar@example.com` | `Customer#2026` |

The seed also adds 7 Canadian airports, 3 airlines, 5 aircraft and 16 flights over the next two weeks (one of them archived). It includes a few bookings for Maya and Omar, and promotions for this month and next. Flight dates are relative to the day the database is created, so reset the database if the demo flights have departed.

---

## Troubleshooting

| Problem | Fix |
| --- | --- |
| The first build is slow or seems to hang | It is downloading Gradle, JDK 21 and the dependencies. Let it finish. |
| `Unsupported class file major version` in your IDE | Set the project SDK to JDK 21. Gradle's copy lives under `~/.gradle/jdks`. |
| The app shows old data after a schema change | Reset the database (see [Resetting the database](#resetting-the-database)). |
| `ArchitectureTest` fails | Your code broke a layering rule, and the failure message names the class. Fix the dependency rather than the test. |
| IntelliJ or VS Code doesn't see the new modules | Reimport or reload the Gradle project. |
