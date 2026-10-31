-- FlyRes schema (SQLite).
-- Money is INTEGER cents; times are ISO-8601 TEXT (yyyy-MM-ddTHH:mm) in local airport time.
-- No migrations: after editing this file, delete ~/.flyres so the database is recreated.
-- DatabaseManager splits this file on semicolons, so keep comments on their own lines
-- and never put a semicolon inside a string literal.

-- Users of every role. Passwords are PBKDF2WithHmacSHA512, 210000 iterations, 256-bit key,
-- with a 16-byte per-user salt; hash and salt are both Base64.
CREATE TABLE users (
    id            INTEGER PRIMARY KEY,
    role          TEXT    NOT NULL CHECK (role IN ('CUSTOMER', 'AGENT', 'ADMIN')),
    email         TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT    NOT NULL,
    password_salt TEXT    NOT NULL,
    first_name    TEXT    NOT NULL,
    last_name     TEXT    NOT NULL,
    phone         TEXT,
    created_at    TEXT    NOT NULL
);

-- Seed data only: there are no management screens for airports or airlines.
CREATE TABLE airports (
    id        INTEGER PRIMARY KEY,
    iata_code TEXT    NOT NULL UNIQUE CHECK (length(iata_code) = 3),
    name      TEXT    NOT NULL,
    city      TEXT    NOT NULL,
    country   TEXT    NOT NULL
);

CREATE TABLE airlines (
    id        INTEGER PRIMARY KEY,
    iata_code TEXT    NOT NULL UNIQUE CHECK (length(iata_code) = 2),
    name      TEXT    NOT NULL
);

-- Seat layout is rows x seats per row, labelled 1A, 1B, ...
CREATE TABLE aircraft (
    id            INTEGER PRIMARY KEY,
    airline_id    INTEGER NOT NULL REFERENCES airlines (id),
    registration  TEXT    NOT NULL UNIQUE,
    model         TEXT    NOT NULL,
    seat_rows     INTEGER NOT NULL CHECK (seat_rows BETWEEN 1 AND 60),
    seats_per_row INTEGER NOT NULL CHECK (seats_per_row BETWEEN 1 AND 10)
);

CREATE TABLE routes (
    id                     INTEGER PRIMARY KEY,
    origin_airport_id      INTEGER NOT NULL REFERENCES airports (id),
    destination_airport_id INTEGER NOT NULL REFERENCES airports (id),
    UNIQUE (origin_airport_id, destination_airport_id),
    CHECK (origin_airport_id <> destination_airport_id)
);

-- Flights are never deleted, only archived. "Departed" is derived from departure_time.
CREATE TABLE flights (
    id             INTEGER PRIMARY KEY,
    flight_number  TEXT    NOT NULL,
    route_id       INTEGER NOT NULL REFERENCES routes (id),
    aircraft_id    INTEGER NOT NULL REFERENCES aircraft (id),
    departure_time TEXT    NOT NULL,
    arrival_time   TEXT    NOT NULL,
    price_cents    INTEGER NOT NULL CHECK (price_cents >= 0),
    status         TEXT    NOT NULL CHECK (status IN ('SCHEDULED', 'ARCHIVED')),
    UNIQUE (flight_number, departure_time),
    CHECK (arrival_time > departure_time)
);

CREATE INDEX ix_flights_departure ON flights (departure_time);
CREATE INDEX ix_flights_route ON flights (route_id);

-- One row per physical seat, generated from the aircraft layout when the flight is created.
CREATE TABLE seats (
    id         INTEGER PRIMARY KEY,
    flight_id  INTEGER NOT NULL REFERENCES flights (id),
    seat_label TEXT    NOT NULL,
    UNIQUE (flight_id, seat_label)
);

-- The customer is the passenger. PENDING lives only in memory and COMPLETED is derived,
-- so only CONFIRMED and CANCELLED are stored. amount_cents is what was actually paid.
CREATE TABLE reservations (
    id                INTEGER PRIMARY KEY,
    confirmation_code TEXT    NOT NULL UNIQUE CHECK (length(confirmation_code) = 6),
    customer_id       INTEGER NOT NULL REFERENCES users (id),
    seat_id           INTEGER NOT NULL REFERENCES seats (id),
    amount_cents      INTEGER NOT NULL CHECK (amount_cents >= 0),
    status            TEXT    NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    created_at        TEXT    NOT NULL,
    cancelled_at      TEXT,
    CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL))
);

-- Makes double booking impossible: at most one CONFIRMED reservation per seat. Never remove.
CREATE UNIQUE INDEX ux_reservations_confirmed_seat ON reservations (seat_id) WHERE status = 'CONFIRMED';

CREATE INDEX ix_reservations_customer ON reservations (customer_id);

-- Simulated card payments. Only the last 4 digits are stored, never the full number or CVV.
-- Failed payments persist nothing, so only COMPLETED and REFUNDED are stored.
CREATE TABLE payments (
    id              INTEGER PRIMARY KEY,
    reservation_id  INTEGER NOT NULL UNIQUE REFERENCES reservations (id),
    cardholder_name TEXT    NOT NULL,
    card_last4      TEXT    NOT NULL CHECK (length(card_last4) = 4 AND card_last4 NOT GLOB '*[^0-9]*'),
    amount_cents    INTEGER NOT NULL CHECK (amount_cents >= 0),
    status          TEXT    NOT NULL CHECK (status IN ('COMPLETED', 'REFUNDED')),
    paid_at         TEXT    NOT NULL,
    refunded_at     TEXT,
    CHECK ((status = 'REFUNDED') = (refunded_at IS NOT NULL))
);

-- Promotions are archived, never deleted. target_month is a YearMonth (yyyy-MM).
CREATE TABLE promotions (
    id           INTEGER PRIMARY KEY,
    title        TEXT    NOT NULL,
    description  TEXT    NOT NULL,
    target_month TEXT    NOT NULL CHECK (target_month GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]'),
    status       TEXT    NOT NULL CHECK (status IN ('SCHEDULED', 'ARCHIVED')),
    created_at   TEXT    NOT NULL
);

CREATE INDEX ix_promotions_month ON promotions (target_month, status);

-- The promotions a customer was shown in a month. Once recorded, the month's set is final.
CREATE TABLE promotion_deliveries (
    id             INTEGER PRIMARY KEY,
    promotion_id   INTEGER NOT NULL REFERENCES promotions (id),
    customer_id    INTEGER NOT NULL REFERENCES users (id),
    delivery_month TEXT    NOT NULL,
    delivered_at   TEXT    NOT NULL,
    UNIQUE (customer_id, promotion_id)
);

CREATE INDEX ix_promotion_deliveries_customer_month ON promotion_deliveries (customer_id, delivery_month);
