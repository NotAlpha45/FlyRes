# ADR-0010: Modify a reservation — change seat

- **Status:** Accepted
- **Date:** 2026-11-27
- **Issue:** FLY-11 (#13)
- **Control(s):** `BookingController` (uses `FlightSearchController` for the seat map)

## Context

Spec §3.2 asks to "cancel or modify existing reservations". The agreed rule is **modify = change seat only**, on the same flight, with no price change. Changing the flight means cancelling and booking again. Other rules that apply:

- The amount paid is fixed at booking.
- The partial unique index `ux_reservations_confirmed_seat` must keep preventing double booking.
- Multi-step writes go through `TransactionRunner`.

## Feature overview

- **Change seat** sits next to Cancel trip on Trip details, for upcoming trips only.
- **The Change seat screen:**
  - Shows the flight's seat map (the reusable `SeatMapView`), with the current seat outlined as **Your seat**.
  - Says "Your seat is 12A. Pick a free seat to move to; your fare stays $329.00."
  - The customer picks a free seat. A **Refresh seats** button reloads availability.
- **Confirming:**
  - The dialog says: "Move from 12A to 3C on WS101? Your fare stays $329.00 and your confirmation code stays MK7Q2D."
  - On confirm, one `changeSeat` call moves the trip, and the app returns to Trip details with "Seat changed to 3C."
- **Stale screens:** if the seat was taken meanwhile, or the trip was cancelled or has departed, the screen says why and refreshes.

## Design

| Layer | Class | Responsibility |
| --- | --- | --- |
| `<<boundary>>` | `ChangeSeatViewController`, `ChangeSeatView.fxml` | Seat map with highlight; confirm; one `changeSeat` call |
| `<<boundary>>` | `TripDetailsViewController` | Opens Change seat with the trip it already loaded |
| `<<control>>` | `BookingController.changeSeat` | Role, ownership, state and seat checks; one transaction |
| `<<entity>>` | `Reservation.changeSeat` | Self-transition on CONFIRMED; same flight; not the current seat |
| Repository | `ReservationRepository.updateSeat` | Guarded update; unique-index collision → seat taken |

Checks in `changeSeat`, in this order, all inside one transaction:

1. Trip exists → `NotFoundException`.
2. It is the actor's → `AccessDeniedException`.
3. Not cancelled → `SEAT_CHANGE_CANCELLED`; not departed → `SEAT_CHANGE_DEPARTED`.
4. New seat exists → `NotFoundException`.
5. It is on the same flight → `SEAT_NOT_ON_FLIGHT`.
6. It isn't the current seat → `SAME_SEAT`.
7. It is free → "Seat X was just taken".

Then `reservation.changeSeat` and `updateSeat`.

## Decisions

### D1. Update the seat in place

- **Options:** cancel and re-book behind the scenes; or update `seat_id` on the existing reservation.
- **Decision:** update in place.
- **Why:**
  - The confirmation code, the amount paid and the payment row must stay the same.
  - A cancel and re-book would refund and re-charge, and issue a new code, which is not what "modify" means to the customer.
  - The reservation state diagram shows this as a self-transition on CONFIRMED.

### D2. Seat problems are `BookingException`s, not field errors

- **Options:** `ValidationException("seat", …)`, as the issue sketched; or `BookingException`.
- **Decision:** `BookingException` for the same seat, a seat on another flight, or a taken seat.
- **Why:**
  - This follows ADR-0007 D5: the screen has no form field for a field error to attach to.
  - A field error would crash `FormValidator`, as it did in FLY-08.
  - As a `BookingException`, the message shows as a toast and the map refreshes.

### D3. The database stays the final guard

- **Decision:** the control checks availability first, for a clear message. `updateSeat` relies on the partial unique index if another booking takes the seat in between, and turns that into the same "Seat X was just taken" message.
- **Why:** a control-level check can't stop two requests racing for one seat. This is the same approach as booking (ADR-0007 D2).

### D4. The map comes from `FlightSearchController.getFlightDetails`

- **Options:** a new seat-map method on `BookingController`; or reuse `getFlightDetails` from FLY-06.
- **Decision:** reuse it.
- **Why:**
  - The data is identical: the flight's seats with availability.
  - It already refuses departed or archived flights for customers, which ends the screen correctly.
  - The customer's own seat shows as taken and outlined, using `SeatMapView.setHighlightedSeat`, which FLY-06 added for exactly this.

### D5. The confirmation uses `ConfirmDialog`

- **Decision:** reuse `ConfirmDialog`, which styles its confirm button as `danger`.
- **Why:**
  - Changing a seat gives up the current one, which another customer may then book, so it deserves the same deliberate confirmation.
  - Reusing the component keeps one confirmation style in the app.

## Consequences

- **FLY-15:** agents changing a seat for a customer reuse `changeSeat` with a widened role check.
- **Reservation state diagram (FLY-23):** add the CONFIRMED → CONFIRMED self-transition "change seat".
- **Limitation:** the seat map isn't live. Another booking shows only after Refresh, and `changeSeat` re-checks anyway.

## Verification

- `ReservationTest`: the move keeps status, code and amount; not allowed when cancelled or pending; a seat on another flight and the same seat are rejected.
- `SqliteReservationRepositoryTest`: `updateSeat` moves the seat and frees the old one; a seat held by another confirmed reservation gives "Seat 1A was just taken" and the old seat is kept; a cancelled reservation throws.
- `BookingControllerTest`:
  - The move keeps amount, code and payment, frees the old seat and takes the new one.
  - Taken seat; same seat; seat on another flight; departed; cancelled; another customer; unknown trip or seat; admin.
- **By hand in the running app** (fresh DB, Maya), MK7Q2D (WS101, 12A):
  - Change seat shows "Your seat is 12A…" with 12A outlined as Your seat.
  - Picking 3C shows "Move from 12A to 3C". The dialog text is as above.
  - Confirming returns to Trip details showing seat 3C, still Confirmed, $329.00, with the toast.
  - In the database: reservation on 3C, CONFIRMED, 32900; payment COMPLETED 32900; 12A has no holder and 3C one.
