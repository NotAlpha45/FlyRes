# ADR-0005: Flight details and seat map

- **Status:** Accepted
- **Date:** 2026-11-22
- **Issue:** FLY-06 (#8)
- **Control(s):** `FlightSearchController`

## Context

Spec §2 requires viewing "flight details", and §3.1 requires displaying available seats. Booking (FLY-08) starts from a chosen seat, change seat (FLY-11) and passenger lists (FLY-16) show the same cabin, and admins need to see any flight. Rules that apply:

- Archived and departed flights are never offered to customers or agents.
- Availability is derived from `CONFIRMED` reservations.
- The view controller calls one control method and holds no business rules.

## Feature overview

- **Select** on a search result opens **Flight details**:
  - Flight number and airline, route with cities, departure, arrival, duration, aircraft model, price and seats left.
  - A seat map below.
- **Seat map:** one button per seat, with row numbers, letters and an aisle. Taken seats are disabled. One seat can be selected, and the summary line shows "Seat 3C · $329.00".
- **Sold out:** a sold-out flight shows "This flight is sold out", and the map is read-only.
- **Refresh seats:** reloads availability and keeps the selection if the seat is still free.
- **Unavailable flight:** if the flight was archived or has departed, a toast says "This flight is no longer available." and the user is taken back to the search.
- **Back to results:** shows the previous search again, re-run so the seat counts are fresh.
- **Continue:** present but disabled (tooltip) until FLY-08 adds payment.

## Design

| Layer | Class | Responsibility |
| --- | --- | --- |
| `<<boundary>>` | `FlightDetailsViewController`, `FlightDetailsView.fxml` | Calls `getFlightDetails` once per load, renders the header and seat map |
| `<<boundary>>` | `FlightSearchViewController` | Select opens details; `restore(criteria)` re-runs a search |
| UI component | `SeatMapView` | Reusable seat grid with selection, highlight and read-only mode |
| `<<control>>` | `FlightSearchController.getFlightDetails` | Role check, not-found and availability rules |
| `<<entity>>` / values | `Seat` (row, letter, `MAP_ORDER`), `SeatAvailability`, `FlightDetails` | Seat-map data; layout derived from the seats |
| Repository | `SeatRepository` / `SqliteSeatRepository` | Seats of a flight with a taken flag, in seat-map order |

## Decisions

### D1. Availability is computed per seat by a query, not stored on the seat

- **Options:** a `taken` column on `seats`; `EXISTS` a `CONFIRMED` reservation.
- **Decision:** `EXISTS` subquery.
- **Why:**
  - It follows the same single source of truth as FLY-05's free-seat count.
  - The partial unique index on `reservations(seat_id) WHERE status = 'CONFIRMED'` already guarantees there is at most one, so no column is needed and nothing can drift.

### D2. Details live in `FlightSearchController`, not `BookingController`

- **Options:** `FlightSearchController`; `BookingController` (created in FLY-08).
- **Decision:** `FlightSearchController.getFlightDetails`.
- **Why:**
  - Viewing a flight is part of "view available flights and flight details" (spec §2). It has no side effects and needs no payment or reservation logic.
  - Booking reads the seat again inside its own transaction anyway.

### D3. Customers and agents get `NotFoundException` for archived or departed flights; admins don't

- **Options:**
  - The same rule for everyone.
  - A `BookingException`.
  - `NotFoundException` with a role exception for admins.
- **Decision:** `NotFoundException("This flight is no longer available.")` for customers and agents. Admins can open any flight.
- **Why:**
  - To a traveller, an archived or departed flight simply doesn't exist any more, which matches search.
  - Admins manage flights (FLY-19/20) and need to see past and archived ones with their seat occupancy.

### D4. The layout comes from the flight's seats, not the aircraft

- **Options:** `aircraft.seatRows × seatsPerRow`; the highest row and letter among the flight's seats.
- **Decision:** derive it in `FlightDetails.of` from the seats.
- **Why:** FLY-17 lets an admin change an aircraft's layout for future flights only. An existing flight must keep drawing the seats it actually has.

### D5. Seat order is numeric by row, then letter

- **Decision:**
  - `Seat.MAP_ORDER` sorts by row number, then letter, so 2A comes before 10A.
  - The SQL orders the same way (`CAST(... AS INTEGER)`).
  - Labels are validated as row 1–99 plus letter A–J.
- **Why:** string order (`10A` < `2A`) would scramble the map and passenger lists.

### D6. The aisle goes after `floor(seatsPerRow / 2)`

- **Decision:** 2–2 (Dash 8), 2–3 (A220), 3–3 (A320/737). No aisle for a single seat per row.
- **Why:** this matches the seeded fleet. It is a display choice only and doesn't affect seat labels.

### D7. "Back to results" passes the search back instead of keeping global state

- **Options:**
  - A session-wide search holder.
  - Caching results.
  - Passing `SearchCriteria` from search → details → back, and re-running the search.
- **Decision:** pass the criteria and re-run with `FlightSearchViewController.restore`.
- **Why:**
  - No new shared state, and the backend stays stateless.
  - The re-run shows fresh seat counts and drops flights that became unavailable.
  - Opening Search from the sidebar starts fresh, by design.

## Consequences

- **FLY-08** enables Continue and receives the flight and the selected `Seat`.
- **FLY-11** reuses `SeatMapView.setHighlightedSeat` for "your seat".
- **FLY-16** can use the read-only mode for passenger lists.
- The seat map shows 360 px of rows and then scrolls. A 60 × 10 cabin (the schema's maximum) is still usable.
- **Limitation:** there is no live update. Another booking shows up only after Refresh, and booking itself re-checks the seat (FLY-08).

## Verification

- `SeatTest`: row and letter parsing; numeric order; invalid labels; `FlightDetails.of` layout, order and counts.
- `SqliteSeatRepositoryTest`: map order including rows 10+; `CONFIRMED` is taken and `CANCELLED` is free; cancelling frees a seat; `findById`.
- `FlightSearchControllerTest`:
  - `getFlightDetails` as customer, agent and admin.
  - Unknown id.
  - Archived as customer; departed as agent.
  - Archived as admin; null actor.
- By hand on a fresh database (`java -Duser.home=<tmp> -jar FlyRes.jar`), as Maya:
  - In search, Select is enabled and opens WS101 YYC → YYZ.
  - The header shows Boeing 737-800, $329.00 and 148 seats left.
  - The map has 150 seats, with 12A and 12B taken.
  - Picking 3C shows "Seat 3C · $329.00".
  - Back to results restores "2 flights · YYC → YYZ with WestJet".
  - Archiving the flight from outside the app, then pressing Refresh, shows the "no longer available" toast and returns to a search with 1 flight.
  - On the Components screen, the seat map shows a 3–3 cabin with taken seats and selection.
