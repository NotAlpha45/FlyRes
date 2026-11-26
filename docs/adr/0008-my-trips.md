# ADR-0008: My trips — reservations and booking history

- **Status:** Accepted
- **Date:** 2026-11-26
- **Issue:** FLY-09 (#11)
- **Control(s):** `BookingController` (uses `PaymentController`)

## Context

Spec §2 asks to "view booking history", and §2.1 gives customers "view reservations". FLY-08 added booking, so customers now need a place to see what they booked, with the payment record. FLY-10 (cancel) and FLY-11 (change seat) will act on a trip from this screen, and FLY-12 shows the same trips to an agent. Rules that apply:

- COMPLETED is **derived** from the departure time and never stored.
- Role checks live in the control.
- The view controller calls one control method per action.
- Only the last 4 card digits are ever shown.

## Feature overview

- **My trips** (sidebar, customers only):
  - Three tabs with counts: **Upcoming**, **Past** and **Cancelled**. Each tab has an empty state.
  - Each trip shows code, flight, route, departure, seat, amount paid and a status badge.
  - **View** or a double-click opens the trip.
- **Trip details:**
  - Confirmation code, status badge, flight and route, departure and arrival, seat, aircraft, and when it was booked.
  - The payment: amount, card `•••• 4242`, cardholder, paid or refunded, with dates.
- **Cancelled trips** explain why:
  - "The airline removed this flight…" when the flight was archived.
  - Otherwise, the date the trip was cancelled.
- **Errors:** opening someone else's trip, or one that doesn't exist, shows the error and returns to My trips.
- **Booking confirmation:** "View my trips" (disabled in FLY-08) now opens My trips.

## Design

| Layer | Class | Responsibility |
| --- | --- | --- |
| `<<boundary>>` | `MyTripsViewController`, `TripDetailsViewController` | One control call each; render tabs and details |
| UI component | `TripTable` | Reusable table of trips with status badges (FLY-12 reuses it) |
| `<<control>>` | `BookingController.listMyReservations`, `getReservation` | Customer-only; own trips only; effective status |
| `<<control>>` | `PaymentController.paymentFor` | Looks up the payment once booking has checked access |
| Domain | `ReservationSummary`, `ReservationDetails`, `MyTrips`, `Reservation.summaryAt` | Effective status, grouping and ordering |
| Repository | `ReservationRepository.findByCustomer` | Every reservation of a customer with flight and seat |

## Decisions

### D1. The control returns trips already grouped (`MyTrips`)

- **Options:**
  - Return a flat list and let the view filter and sort per tab.
  - Return `MyTrips(upcoming, past, cancelled)`.
- **Decision:** `MyTrips.of(summaries)` groups and sorts in the domain:
  - Upcoming: soonest departure first.
  - Past: most recent departure first.
  - Cancelled: most recently cancelled first.
- **Why:**
  - Deciding which trip is "past" or "upcoming" is a business rule (it depends on the clock), so it doesn't belong in a view controller (rule 6).
  - It is unit-tested without the UI.

### D2. A summary wraps the entity instead of copying its fields

- **Options:**
  - A flat record with every column (flight number, airline, route, …), as the issue sketched.
  - `ReservationSummary(Reservation, effectiveStatus)`.
- **Decision:** wrap the entity, plus `isFlightRemoved()` (CANCELLED and flight ARCHIVED).
- **Why:**
  - The `Reservation` already carries its flight, route, aircraft and seat, so copying them into a flat record would duplicate the model.
  - The record adds only what the entity can't know by itself: the effective status at a given clock.

### D3. The effective status comes from `Flight.isDeparted`

- **Decision:** `Reservation.statusAt` and `summaryAt` use `Flight.isDeparted(clock)`, so a trip departing at or before now is COMPLETED.
- **Why:** this is the same rule as search, flight details and booking, kept in one place.

### D4. Another customer's trip is `AccessDeniedException`, not `NotFoundException`

- **Options:** pretend it doesn't exist; deny access.
- **Decision:** an unknown id gives `NotFoundException(TRIP_NOT_FOUND)`. Another customer's id gives `AccessDeniedException("You can only view your own trips.")`.
- **Why:**
  - It matches the issue and the rule that roles and ownership are enforced in the control.
  - The view handles both the same way: a toast, then back to My trips.

### D5. Payment lookup goes through `PaymentController`

- **Options:**
  - Inject `PaymentRepository` into `BookingController`.
  - Add `PaymentController.paymentFor(reservationId)`.
- **Decision:** `paymentFor`, called only after `BookingController` has checked ownership.
- **Why:** `BookingController` already talks to payments through `PaymentController` (FLY-08). Keeping one path avoids a second dependency on the payment table.

### D6. The confirmation code reuses the existing style

- **Decision:**
  - Trip details shows the code with FLY-08's `booking-code` style.
  - The new styles (`status-badge`, `trip-notice`, `tab-body`) use only AtlantaFX colour variables and the 8 px spacing scale.
- **Why:** a confirmation code should look the same wherever it appears, with no second style for the same thing.

## Consequences

- **FLY-10 / FLY-11:** add Cancel trip and Change seat buttons in `TripDetailsView`'s `actionRow`, shown for upcoming trips.
- **FLY-12 / FLY-15:** reuse `TripTable` and add an agent-facing `listReservationsForCustomer`.
- **Limitation:** trips load one flight per reservation (N+1 queries). That's fine at course-project scale, and noted here in case it matters later.

## Verification

- `MyTripsTest`: grouping and ordering of all three groups; departing exactly now is COMPLETED; flight removed vs cancelled by the customer; empty.
- `SqliteReservationRepositoryTest`: `findByCustomer` returns all of a customer's reservations with flight and seat, and none for another customer.
- `BookingControllerTest`:
  - `listMyReservations` groups the customer's own trips, including a departed one (Past) and an archived-flight one (flight removed).
  - Other customers see nothing; admins are denied.
  - `getReservation` includes the payment; another customer is denied; an unknown id is not found; admins are denied.
- **Screens, rendered offscreen** from the real router and controls on the seed data (a scratch harness that never shows a window, so it couldn't take focus from other apps):
  - Maya's My trips shows "Upcoming (2)" with MK7Q2D and MK9F4T.
  - Her trip details show Confirmed, $329.00 and `•••• 4242`, Paid.
  - Omar's OM5R1X shows "Flight removed", the refund notice, and Refunded with both dates.
  - Omar opening Maya's trip returns to My trips with "You can only view your own trips."
