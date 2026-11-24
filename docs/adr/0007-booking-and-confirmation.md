# ADR-0007: Booking and confirmation

- **Status:** Accepted
- **Date:** 2026-11-23
- **Issue:** FLY-08
- **Control(s):** `BookingController`

## Context

Booking connects flight details (FLY-06) and simulated card payment (FLY-07). The selected seat and displayed fare may be stale by the time the customer pays. A successful booking must persist one `CONFIRMED` reservation and one completed payment, while any failure leaves neither record.

## Feature overview

- A customer continues from a selected seat to a payment screen with flight, route, departure, seat and fare.
- `BookingController.book` rechecks flight status and departure, seat ownership and availability, duplicate customer bookings, and the current fare before processing payment.
- The reservation insert and payment insert run in one `TransactionRunner` transaction.
- A successful booking displays a six-character confirmation code and the saved fare, seat and payment last four digits.

## Decisions

### D1. The booking use case owns the transaction

- **Options:** save payment during authorization and compensate if booking fails; or keep the payment transient until the reservation exists.
- **Decision:** call `processPayment` for an unsaved payment, insert the confirmed reservation, then call `recordPayment`, all inside one transaction.
- **Why:** payment failure creates no records, and any persistence failure rolls back both rows. Payment is simulated, so no external charge needs compensation.

### D2. The database is authoritative for seat races

- **Decision:** check current availability for a clear early error, then rely on `ux_reservations_confirmed_seat` to reject a concurrent booking. Translate that constraint failure into a seat-specific `BookingException`.
- **Why:** a control-level check alone cannot prevent two requests from selecting the same seat.

### D3. Protect the fare the customer reviewed

- **Decision:** compare `expectedPriceCents` with the current flight price before processing the card; reject with the current fare when they differ.
- **Why:** the customer is never charged an amount they did not see.

### D4. Generate short human-readable confirmation codes

- **Decision:** use six characters from `23456789ABCDEFGHJKLMNPQRSTUVWXYZ`, checking repository collisions and retrying up to ten times. Production wiring uses `SecureRandom`; tests inject a seeded `Random`.
- **Why:** codes are compact and avoid easily confused characters while remaining deterministic in tests.

## Consequences

- Agents, cancellations, seat changes, and trips history remain out of scope (FLY-09 through FLY-11 and FLY-15).
- The payment form clears the CVV after a decline while retaining the other entered values.
- `COMPLETED` remains a derived reservation state; only `CONFIRMED` and `CANCELLED` are persisted.

## Verification

- `ReservationTest`: confirm/cancel transitions, illegal transitions, and completion at departure.
- Confirmation-code alphabet and collision retry are covered in `BookingControllerTest`.
- `BookingControllerTest`: success and payment linkage; seat taken, decline rollback, departed/archived flight, mismatched seat, fare change, duplicate customer booking, and role/customer ownership checks.
- `SqliteReservationRepositoryTest`: round trip, code/flight queries, and seat-index collision translation.
- Manually verify the customer path from flight details through payment to confirmation, then refresh search to confirm availability changes.