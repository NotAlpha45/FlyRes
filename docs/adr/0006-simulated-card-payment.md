# ADR-0006: Simulated card payment

- **Status:** Accepted
- **Date:** 2026-11-23
- **Issue:** FLY-07 (#9)
- **Control(s):** `PaymentController`

## Context

Spec §3.5 asks to "record payment information" with simulated processing. §7.1 E requires a state diagram for the payment process. Booking (FLY-08) charges a card, and cancelling (FLY-10) and archiving a flight (FLY-20) refund it. Rules that constrain the design:

- Store only the cardholder name, the **last 4 digits**, the amount, the status and the timestamps. **Never persist or log the full number or the CVV.**
- Validation: Luhn check, an expiry in the future, a 3-digit CVV.
- Any number ending in `0002` declines, for demos.
- Transitions: `INITIATED → AUTHORIZED → COMPLETED` or `→ FAILED`, and `COMPLETED → REFUNDED`. Illegal transitions throw `IllegalStateException`.
- If payment fails, nothing is persisted.

## Feature overview

- **`PaymentController.processPayment(actor, details, amount)`** (customers and agents):
  - It validates every card field at once (name, number, expiry, CVV) and reports each error under its field.
  - It then authorizes the card and returns a COMPLETED payment, or throws "Your card was declined. No charge was made."
  - Nothing is saved at this point.
- **`recordPayment(reservationId, payment)`** saves a completed payment. Only the booking calls it, inside its own transaction (FLY-08).
- **`PaymentForm`** is the reusable card form for the booking screen: name, card number grouped in fours, expiry month and year, masked CVV.
- **Components screen:** the form calls the real controller and saves nothing, so the rules can be tried by hand.

## Design

| Layer | Class | Responsibility |
| --- | --- | --- |
| `<<boundary>>` support | `PaymentForm` | Collects input, shows the control's field errors, clears the CVV after a decline |
| `<<control>>` | `PaymentController` | Role check, run the strategy, drive the state machine, save on request |
| Strategy | `PaymentMethod` ← `CardPayment` | Validation and (simulated) authorization for one way of paying |
| `<<entity>>` | `Payment`, `PaymentStatus` | State machine; holds only name, last 4, amount, timestamps |
| Value object | `PaymentDetails` | What the user typed; masked `toString()` |
| Repository | `PaymentRepository` / `SqlitePaymentRepository` | Saves and loads COMPLETED/REFUNDED payments |

The state diagram is `docs/diagrams/payment-state.puml`.

## Decisions

### D1. Strategy for the payment method

- **Options:**
  - Card logic inside `PaymentController`.
  - A `PaymentMethod` interface with `CardPayment` as its only implementation for now.
- **Decision:** the Strategy. `PaymentController` has an overload that takes any `PaymentMethod`; the `PaymentDetails` overload creates a `CardPayment`.
- **Why:**
  - The control stays the same when a new way to pay is added (spec §4 scalability).
  - The pattern is visible in the class diagram.
  - A test pays through the interface with a fake voucher.

### D2. State pattern as methods on `Payment`

- **Options:**
  - A status field set freely by the control.
  - One class per state.
  - Guarded transition methods on the entity.
- **Decision:** guarded methods (`authorize`, `complete`, `fail`, `refund`) that throw `IllegalStateException` on any transition not in the diagram.
- **Why:**
  - The rules live in one place, they are fully unit-tested (every legal and illegal transition), and they map one-to-one onto the state diagram.
  - Classes per state would add five tiny classes for no extra behaviour.

### D3. `processPayment` doesn't save; `recordPayment` saves inside the booking transaction

- **Options:**
  - Save in `processPayment` and delete if the booking fails.
  - Return the payment unsaved and let the booking save it.
- **Decision:** return it unsaved. The booking calls `recordPayment` after inserting the reservation, in one `TransactionRunner` transaction.
- **Why:**
  - "If payment fails, nothing is persisted," and payments are never hard-deleted.
  - The schema also requires a payment to reference an existing reservation (`reservation_id NOT NULL UNIQUE`).
  - A declined payment never exists in the database, which matches the schema allowing only `COMPLETED` and `REFUNDED`.

### D4. Card data never travels past the control

- **Decision:**
  - `CardPayment` reduces the number to `last4()`, and `Payment` has no field for the number or the CVV.
  - `PaymentDetails.toString()` masks the number and hides the CVV, and error messages never include the card data.
  - The `payments` table has no column that could hold the full number or the CVV, and a test pins the column list.
- **Why:** with this design, a log line or exception message can't leak card data by accident.

### D5. Validation rules

- **Decision:**
  - The number may contain spaces and dashes and must have 13–19 digits and pass Luhn.
  - The card is valid **through** its expiry month (`expiry >= current month`, from the injected `Clock`).
  - The CVV is exactly 3 digits.
  - The name is required, at most 50 characters.
  - Every invalid field is reported at once, in form order.
- **Why:** this matches real card behaviour closely enough for a demo, gives each field its own message, and is deterministic in tests (December → January is covered).

### D6. Expiry as month and year pickers

- **Options:** a free-text `MM/YY` field; two pickers.
- **Decision:** month and year combo boxes. The year list starts at the current year.
- **Why:**
  - There is no parsing error to explain.
  - The control still decides whether the card has expired.

## Consequences

- **FLY-08** calls `processPayment`, then `recordPayment`, in its booking transaction, and puts `PaymentForm` on the booking screen.
- **FLY-10** and **FLY-20** use `Payment.refund` and add `PaymentRepository.markRefunded`.
- **FLY-12** (payment records) reads saved payments.
- **Limitations, by design:**
  - Single payment method.
  - No partial refunds or fees.
  - Authorization is simulated.

## Verification

- `PaymentTest`: every legal transition; illegal ones (complete from INITIATED; refund from INITIATED or AUTHORIZED; anything from FAILED or REFUNDED; double authorize; fail from COMPLETED); amount and last-4 checks; masked `toString` for `PaymentDetails` and `Payment`.
- `CardPaymentTest`:
  - Luhn-valid 13–19 digit numbers, including spaces and dashes.
  - Luhn failure; wrong length or letters.
  - Expiry this month, last month, and December → January.
  - CVV lengths; blank or long name; every field at once.
  - Decline on `0002`; normalised last 4 and name; `toString` never shows the number or CVV.
- `SqlitePaymentRepositoryTest`: round trip; only the last 4 stored (column list pinned); non-COMPLETED rejected; one payment per reservation.
- `PaymentControllerTest`:
  - A valid card completes unsaved.
  - An agent can pay.
  - A decline throws and saves nothing.
  - An invalid card reports all fields, and messages contain no card data.
  - Admins are denied; non-positive amounts are rejected.
  - A custom `PaymentMethod` works through the interface.
  - `recordPayment` saves; a non-completed payment throws.
- By hand on the Components screen (fresh database, Maya):
  - Empty submit → four inline errors.
  - Card number grouped as typed.
  - `4242 4242 4242 4241` → "This card number is not valid."
  - `4242…4242` → approved toast.
  - `4000…0002` → decline toast, and the CVV is cleared.
  - Afterwards the database still has only the 4 seeded payments, and the app log contains no card digits.
