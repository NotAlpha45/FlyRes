# ADR-0009: Cancel a reservation with a full refund

- **Status:** Accepted
- **Date:** 2026-11-27
- **Issue:** FLY-10 (#12)
- **Control(s):** `BookingController` (uses `PaymentController`)

## Context

Spec §3.2 asks to "cancel … existing reservations", and §2.1 gives customers "cancel bookings". The agreed rules:

- **Cancel = always a full refund, with no fees.**
- A reservation keeps the amount paid. Later price changes never affect it.
- Every multi-step write goes through `TransactionRunner`.
- Destructive actions ask for confirmation and say what will happen.
- The reservation and payment state machines must match their diagrams (CONFIRMED → CANCELLED; COMPLETED → REFUNDED).

## Feature overview

- **Cancel trip** appears on Trip details (FLY-09) for **upcoming** trips only.
- The confirmation dialog says exactly what happens: "Cancel WS101 YYC → YYZ on Sun 29 Nov, 08:15? $329.00 will be refunded in full to the card ending 4242."
  - "Cancel" is the default button.
- After confirming:
  - The trip shows **Cancelled**, with a notice giving the cancellation date.
  - The payment shows **Refunded**, with a refund date.
  - A toast says "Trip cancelled. $329.00 refunded."
  - The seat is free to book again.
- **Stale screens:** if the trip was already cancelled or has departed meanwhile, the error is shown and the trip reloaded as it is now.

## Design

| Layer | Class | Responsibility |
| --- | --- | --- |
| `<<boundary>>` | `TripDetailsViewController` | Confirm with `ConfirmDialog`, one `cancelReservation` call, re-render |
| `<<control>>` | `BookingController.cancelReservation` | Role, ownership, state checks; one transaction |
| `<<control>>` | `PaymentController.refund` | `Payment.refund(now)` plus `markRefunded`, inside the caller's transaction |
| `<<entity>>` | `Reservation.cancel`, `Payment.refund` | Guarded state transitions |
| Repository | `ReservationRepository.markCancelled`, `PaymentRepository.markRefunded` | Guarded `UPDATE … WHERE status = …` |

Sequence of `cancelReservation`, all inside `TransactionRunner`:

1. `findById` → `NotFoundException` if unknown.
2. Ownership → `AccessDeniedException` if it belongs to someone else.
3. Effective status: CANCELLED → `ALREADY_CANCELLED`; COMPLETED (departed) → `ALREADY_DEPARTED`.
4. `reservation.cancel(now)` → `markCancelled`.
5. `payments.refund(id)`.
6. Return `ReservationDetails` (cancelled reservation plus refunded payment).

## Decisions

### D1. One transaction for both rows; the payment update goes last

- **Decision:** cancel the reservation, then refund the payment, in a single `TransactionRunner` call. A failure in either rolls both back.
- **Why:**
  - A trip must never be cancelled without a refund, or refunded while still confirmed.
  - The test `cancelReservation_refundFails_rollsBackTheCancellation` uses a payment repository that throws on refund, and checks that the reservation stays CONFIRMED and the payment COMPLETED.

### D2. Guarded updates in the repository as well as the entity

- **Options:** trust the control's checks; or also guard the SQL (`WHERE status = 'CONFIRMED'` / `'COMPLETED'`) and treat "0 rows updated" as an error.
- **Decision:** both.
- **Why:** the entity checks catch logic errors early. The guarded `UPDATE` makes a double cancel or a double refund impossible, even if two screens act on the same trip.

### D3. The refund is the stored amount paid

- **Decision:** `refund` changes the existing payment row's status. It never recalculates from the flight.
- **Why:** this is the "the amount paid is kept" rule. A test raises the flight price before cancelling and checks that the refund is still the original $159.00.

### D4. `cancelReservation` returns the updated trip

- **Options:** return the `Reservation`, as the issue sketched; or return `ReservationDetails`.
- **Decision:** `ReservationDetails` (cancelled reservation plus refunded payment).
- **Why:** the view re-renders from one control call, with no second read, and the summary carries the effective status the screen already uses (FLY-09).

### D5. Departed trips can't be cancelled

- **Decision:** a trip whose effective status is COMPLETED is refused with `ALREADY_DEPARTED`. The button is hidden for past trips, and the control refuses them anyway.
- **Why:** a full refund for a flight already taken isn't a cancellation. Hiding a button is cosmetic; the rule lives in the control (rule 7).

## Consequences

- **FLY-15:** agents cancelling for a customer reuse `cancelReservation` with a widened role check.
- **FLY-20:** archiving a flight reuses `Reservation.cancel`, `markCancelled` and `PaymentController.refund` for every confirmed reservation.
- The reservation and payment state diagrams (FLY-23) must show CONFIRMED → CANCELLED and COMPLETED → REFUNDED, triggered by "cancel trip".

## Verification

- `SqliteReservationRepositoryTest`: `markCancelled` cancels and frees the seat (it can be booked again); cancelling twice throws.
- `SqlitePaymentRepositoryTest`: `markRefunded` records the refund and keeps the amount; a second refund, or a refund with no payment, throws.
- `PaymentControllerTest`: `refund` marks the payment refunded at "now"; twice throws; no payment throws.
- `BookingControllerTest`:
  - Cancel an upcoming trip: cancelled, refunded, seat free.
  - Already cancelled is refused, and the payment is refunded only once.
  - Departed, another customer, unknown id and admin are all refused.
  - The refund uses the stored amount, not the current price.
  - A failed refund rolls everything back.
- **By hand in the running app** (fresh DB, Maya), on MK7Q2D:
  - The dialog text is as above, and confirming shows Cancelled, the notice, Refunded and the toast.
  - My trips shows Upcoming (1) and Cancelled (1).
  - In the database: reservation CANCELLED with `cancelled_at`, payment REFUNDED for 32900 with `refunded_at`, and seat 12A has no confirmed holder.
