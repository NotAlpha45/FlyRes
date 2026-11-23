# ADR-0004: Flight search

- **Status:** Accepted
- **Date:** 2026-11-22
- **Issue:** FLY-05 (#6)
- **Control(s):** `FlightSearchController`

## Context

Spec §3.1 requires searching flights by **origin, destination, date and airline**, and showing available seats, flight times and prices. Booking (FLY-06/08), schedules (FLY-16) and flight management (FLY-17–20) all build on the same flight model, so this issue adds the read side of `domain.flight`.

Rules that constrain the design:

- Archived and departed flights never appear in search.
- Money is `long` cents, formatted only in the UI.
- Times are local airport time stored as ISO-8601 text.
- Role checks live in the control.
- `Clock` is injected.

## Feature overview

- **Customers and agents** see "Search flights" in the sidebar; admins don't, and the control rejects them anyway.
- The search form has three required or optional fields and a filter:
  - **From** and **To** pickers (required), with a Swap button.
  - An optional **date**; past days can't be picked.
  - An optional **airline**, "All airlines" by default.
- **Search** runs one control call:
  - Field errors (missing or same airports, past date, unknown airline) appear inline, and no search runs.
  - Results appear in a `FlightTable` sorted by departure: flight, airline, route, departure, arrival (`+1` when overnight), duration, price in dollars and seats left.
  - An empty result shows "No flights found for YYC → YVR on … with …".
- Each row has a **Select** button, disabled with a tooltip until FLY-06 adds flight details and seat selection.

## Design

| Layer | Class | Responsibility |
| --- | --- | --- |
| `<<boundary>>` | `FlightSearchViewController`, `FlightSearchView.fxml` | Reads the form, calls `searchFlights` once, shows results or field errors |
| UI component | `FlightTable`, `Formats` | Reusable results table; cents and `java.time` → text |
| `<<control>>` | `FlightSearchController` | Role check, validation and normalisation, date window, departed filter |
| `<<control>>` helper | `Access` | One shared role check for every control |
| `<<entity>>` | `Airport`, `Airline`, `Aircraft`, `Route`, `Seat`, `Flight`, `FlightStatus` | Flight model; `Flight.isDeparted(Clock)` |
| Value objects | `SearchCriteria`, `FlightSummary` | Search input; a flight plus its free-seat count |
| Repository | `FlightRepository`, `AirportRepository`, `AirlineRepository` | Interfaces used by the control |
| Data | `SqliteFlightRepository`, `SqliteAirportRepository`, `SqliteAirlineRepository` | SQL with `PreparedStatement` |

Main flow:

1. `FlightSearchViewController.onSearch()` calls `FlightSearchController.searchFlights(user, criteria)`.
2. The control checks the role (`Access.require`), validates the criteria, and works out the time window.
3. `FlightRepository.search(...)` runs one joined query.
4. The control drops departed flights and returns the results.
5. `FlightTable` displays them.

## Decisions

### D1. Free seats are counted in the query, not stored

- **Options:**
  - A seats-left column on `flights`, updated on every booking or cancellation.
  - Counting at read time.
- **Decision:** count at read time: seats on the flight with no `CONFIRMED` reservation (a correlated `NOT EXISTS`).
- **Why:**
  - There is a single source of truth, the reservations table.
  - Nothing can drift when a booking is cancelled or a flight is archived.
  - The data is small (≤ 600 seats per flight), so the cost is negligible.

### D2. The repository filters by date window; the control applies "departed"

- **Options:**
  - Compare the departure with "now" in SQL.
  - Pass a date window to SQL and filter `isDeparted(clock)` in Java.
- **Decision:** the repository takes `[from, toExclusive)` plus status, route and airline. The control removes departed flights with `Flight.isDeparted(clock)`.
- **Why:**
  - The "at or before now is departed" rule lives in one domain method, tested with a fixed `Clock`. FLY-06, FLY-09 and FLY-16 reuse it.
  - String comparison in SQL would need care at minute boundaries.
  - A search with no date starts at the current minute, so SQL already skips most past flights.

### D3. Codes are normalised in the control and validated against the database

- **Decision:**
  - Origin, destination and airline codes are trimmed and upper-cased.
  - Blank means "not given" (for the airline, all airlines).
  - Unknown codes are field errors.
- **Why:** the boundary may pass anything, but the control owns the rules. The same validation protects later callers (agent booking, schedules) without the UI's pickers.

### D4. "All airlines" is a sentinel item in the picker

- **Options:** a nullable picker value; a separate checkbox; a sentinel `Airline` with a blank code.
- **Decision:** a sentinel item whose blank code the control reads as "no filter".
- **Why:** the picker always shows a readable value, and no extra control is needed. The view stays a thin pass-through.

### D5. Airports, airlines, aircraft, routes and seats are records; `Flight` is a class

- **Decision:** the immutable reference data are Java records. `Flight` is a regular class with identity-based `equals`, because it has state (status) that later issues change (archive).
- **Why:** this follows the team convention: records for immutable values, classes for entities with state transitions.

### D6. `Access` holds the role check for every control

- **Decision:** a package-private `Access.require(actor, action, roles...)` throws `AccessDeniedException`, also for a null actor.
- **Why:** every later control (booking, payments, management, promotions) needs the same check. One helper keeps the message and the null handling consistent.

## Consequences

- FLY-06 enables the Select action and reuses `FlightRepository.findById`, `Flight.isDeparted` and `Seat`.
- FLY-16 (schedules) reuses `FlightTable` and the search query's joins. FLY-17 reuses `AirlineRepository`.
- **Limitations, by design:**
  - One-way, single-leg search only.
  - No price or time filters.
  - No sorting controls (results are always by departure).
  - Times are local airport time with no time-zone conversion.

## Verification

- `FlightTest`: departed boundary (at now = departed), duration, airline, invalid schedule; `Aircraft.capacity`; `Route` with the same airport.
- `SqliteFlightRepositoryTest`:
  - Route and date-window search; archived flights excluded.
  - Airline filter.
  - Free seats = capacity minus `CONFIRMED`, and cancelling a reservation frees its seat.
  - `findById` maps every field.
  - Airport and airline lookups.
- `FlightSearchControllerTest` (fixed `Clock`):
  - Sorting; date filter; flights departed today and archived flights excluded.
  - Airline filter; code normalisation; a blank airline means all airlines.
  - Every validation error.
  - Admin and null actor rejected.
- `FlyResBackendFlightSearchTest`: the acceptance examples on the real seed data, through the facade.
- `FormatsTest`: money, dates, overnight arrival and durations.
- By hand on a fresh database (`java -Duser.home=<tmp> -jar FlyRes.jar`):
  - The sidebar shows "Search flights" for customer and agent and hides it for admin.
  - Searching with no airports picked shows both inline errors; the same airport twice shows the destination error.
  - YYC → YVR lists both AC211 flights sorted, at $189.00 and $179.00 with 100 seats.
  - YYC → YYZ lists WS101 (148 seats), F8601 and WS101; the archived F8603 never appears.
  - Filtering by Flair shows only F8601.
  - The empty state names the airline.
