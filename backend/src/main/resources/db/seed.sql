-- FlyRes demo data, loaded once when the database file is first created.
-- All dates are relative to today, so the demo never expires.
-- Keep the demo accounts in sync with the README.
-- DatabaseManager splits this file on semicolons, so keep comments on their own lines
-- and never put a semicolon inside a string literal.

-- Demo accounts. Passwords: Admin#2026, Agent#2026, Customer#2026 (both customers).
INSERT INTO users (id, role, email, password_hash, password_salt, first_name, last_name, phone, created_at) VALUES
    (1, 'ADMIN',    'admin@flyres.ca',  '0ppbLYONUmsAtv2SWbjJ1+EHMV8Wm5h3pBWpe2zM2aI=', 'Ha9g46wURZw/PB8VVmdcaw==', 'Alex',   'Morgan', NULL,           strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-90 days')),
    (2, 'AGENT',    'agent@flyres.ca',  'SPfrjjjdAZ0zLtCAWDkliTf173oaXSq4ULZ/VBkxCe8=', 'pvqGeYi9I1GD3xayiH38HA==', 'Jordan', 'Lee',    NULL,           strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-90 days')),
    (3, 'CUSTOMER', 'maya@example.com', 'oA1PgbPqKgmgktsXAMsStWoQK4yHNzDdIqqMPyxVh4I=', 'pciPyn/5ob2m72rHyn337A==', 'Maya',   'Patel',  '403-555-0142', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-60 days')),
    (4, 'CUSTOMER', 'omar@example.com', '4wsUnbT4gu/rStpOagz3Dpus3t2tKc1obNjMOYC7rLk=', 'AStOazim+f58E5ZXH3seqw==', 'Omar',   'Haddad', '587-555-0199', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-45 days'));

INSERT INTO airports (id, iata_code, name, city, country) VALUES
    (1, 'YYC', 'Calgary International Airport',                    'Calgary',   'Canada'),
    (2, 'YVR', 'Vancouver International Airport',                  'Vancouver', 'Canada'),
    (3, 'YYZ', 'Toronto Pearson International Airport',            'Toronto',   'Canada'),
    (4, 'YUL', 'Montréal-Trudeau International Airport',           'Montréal',  'Canada'),
    (5, 'YEG', 'Edmonton International Airport',                   'Edmonton',  'Canada'),
    (6, 'YOW', 'Ottawa Macdonald-Cartier International Airport',   'Ottawa',    'Canada'),
    (7, 'YHZ', 'Halifax Stanfield International Airport',          'Halifax',   'Canada');

INSERT INTO airlines (id, iata_code, name) VALUES
    (1, 'AC', 'Air Canada'),
    (2, 'WS', 'WestJet'),
    (3, 'F8', 'Flair Airlines');

INSERT INTO aircraft (id, airline_id, registration, model, seat_rows, seats_per_row) VALUES
    (1, 1, 'C-FDAC', 'Airbus A220-300',  20, 5),
    (2, 1, 'C-FKAC', 'Airbus A320',      24, 6),
    (3, 2, 'C-GWSA', 'Boeing 737-800',   25, 6),
    (4, 2, 'C-GWSB', 'De Havilland Dash 8-400', 18, 4),
    (5, 3, 'C-FLFA', 'Boeing 737 MAX 8', 26, 6);

INSERT INTO routes (id, origin_airport_id, destination_airport_id) VALUES
    (1, 1, 2),
    (2, 2, 1),
    (3, 1, 3),
    (4, 3, 1),
    (5, 1, 5),
    (6, 5, 1),
    (7, 3, 4),
    (8, 4, 3),
    (9, 2, 3),
    (10, 3, 7),
    (11, 6, 1);

-- Times are 'start of day' plus a day offset plus a clock time, in local time.
-- Every flight is in the future. Flight 16 is archived, so it must not appear in search.
INSERT INTO flights (id, flight_number, route_id, aircraft_id, departure_time, arrival_time, price_cents, status) VALUES
    (1,  'AC211',  1,  1, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+1 days',  '+07:00'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+1 days',  '+08:30'), 18900, 'SCHEDULED'),
    (2,  'AC214',  2,  1, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+1 days',  '+18:00'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+1 days',  '+19:30'), 18900, 'SCHEDULED'),
    (3,  'WS101',  3,  3, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+2 days',  '+08:15'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+2 days',  '+12:10'), 32900, 'SCHEDULED'),
    (4,  'WS102',  4,  3, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+4 days',  '+13:30'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+4 days',  '+17:20'), 31900, 'SCHEDULED'),
    (5,  'WS3301', 5,  4, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+2 days',  '+09:00'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+2 days',  '+09:50'), 11900, 'SCHEDULED'),
    (6,  'WS3302', 6,  4, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+2 days',  '+17:00'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+2 days',  '+17:50'), 11900, 'SCHEDULED'),
    (7,  'F8601',  3,  5, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+3 days',  '+06:45'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+3 days',  '+10:40'), 21900, 'SCHEDULED'),
    (8,  'AC405',  7,  2, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+5 days',  '+11:00'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+5 days',  '+12:20'), 16900, 'SCHEDULED'),
    (9,  'AC406',  8,  2, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+5 days',  '+15:00'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+5 days',  '+16:20'), 16900, 'SCHEDULED'),
    (10, 'F8711',  9,  5, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+6 days',  '+07:30'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+6 days',  '+12:30'), 27900, 'SCHEDULED'),
    (11, 'AC620',  10, 2, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+8 days',  '+09:30'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+8 days',  '+12:15'), 24900, 'SCHEDULED'),
    (12, 'WS111',  11, 3, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+10 days', '+10:00'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+10 days', '+12:10'), 29900, 'SCHEDULED'),
    (13, 'AC211',  1,  1, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+7 days',  '+07:00'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+7 days',  '+08:30'), 17900, 'SCHEDULED'),
    (14, 'AC214',  2,  1, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+7 days',  '+18:00'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+7 days',  '+19:30'), 17900, 'SCHEDULED'),
    (15, 'WS101',  3,  3, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+14 days', '+08:15'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+14 days', '+12:10'), 30900, 'SCHEDULED'),
    (16, 'F8603',  3,  5, strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+9 days',  '+06:45'), strftime('%Y-%m-%dT%H:%M', 'now', 'localtime', 'start of day', '+9 days',  '+10:40'), 21900, 'ARCHIVED');

-- One seat per position in each flight's aircraft layout: 1A, 1B, ... (same rule as createFlight).
WITH RECURSIVE
    seat_row (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seat_row WHERE n < 60),
    seat_col (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seat_col WHERE n < 10)
INSERT INTO seats (flight_id, seat_label)
SELECT f.id, seat_row.n || substr('ABCDEFGHIJ', seat_col.n, 1)
FROM flights f
JOIN aircraft a ON a.id = f.aircraft_id
JOIN seat_row ON seat_row.n <= a.seat_rows
JOIN seat_col ON seat_col.n <= a.seats_per_row
ORDER BY f.id, seat_row.n, seat_col.n;

-- Reservation 4 was on the archived flight, so it was cancelled and refunded.
INSERT INTO reservations (id, confirmation_code, customer_id, seat_id, amount_cents, status, created_at, cancelled_at) VALUES
    (1, 'MK7Q2D', 3, (SELECT id FROM seats WHERE flight_id = 3  AND seat_label = '12A'), 32900, 'CONFIRMED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-6 days'),  NULL),
    (2, 'MK9F4T', 3, (SELECT id FROM seats WHERE flight_id = 8  AND seat_label = '7C'),  16900, 'CONFIRMED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-3 days'),  NULL),
    (3, 'OM3H8P', 4, (SELECT id FROM seats WHERE flight_id = 3  AND seat_label = '12B'), 32900, 'CONFIRMED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-6 days'),  NULL),
    (4, 'OM5R1X', 4, (SELECT id FROM seats WHERE flight_id = 16 AND seat_label = '9A'),  21900, 'CANCELLED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-10 days'), strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-1 days'));

INSERT INTO payments (id, reservation_id, cardholder_name, card_last4, amount_cents, status, paid_at, refunded_at) VALUES
    (1, 1, 'Maya Patel', '4242', 32900, 'COMPLETED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-6 days'),  NULL),
    (2, 2, 'Maya Patel', '4242', 16900, 'COMPLETED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-3 days'),  NULL),
    (3, 3, 'Omar Haddad', '1881', 32900, 'COMPLETED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-6 days'),  NULL),
    (4, 4, 'Omar Haddad', '1881', 21900, 'REFUNDED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-10 days'), strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-1 days'));

-- Four promotions this month, so each customer gets a random three. Two next month, one archived.
-- No deliveries are seeded: each customer's first login of the month picks their set.
INSERT INTO promotions (id, title, description, target_month, status, created_at) VALUES
    (1, 'Rockies weekend getaway', 'Save 15% on return flights between Calgary and Vancouver booked this month.', strftime('%Y-%m', 'now', 'localtime'), 'SCHEDULED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-30 days')),
    (2, 'Fly east for less',       'Lower fares on Toronto and Montréal departures all month long.',               strftime('%Y-%m', 'now', 'localtime'), 'SCHEDULED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-30 days')),
    (3, 'Early bird Tuesdays',     'Morning flights on Tuesdays come with a free checked bag.',                    strftime('%Y-%m', 'now', 'localtime'), 'SCHEDULED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-30 days')),
    (4, 'Prairie hopper',          'Calgary to Edmonton from $99 one way.',                                        strftime('%Y-%m', 'now', 'localtime'), 'SCHEDULED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-30 days')),
    (5, 'Holiday season preview',  'Book your holiday trips early and lock in today''s fares.',               strftime('%Y-%m', 'now', 'localtime', 'start of month', '+1 months'), 'SCHEDULED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-7 days')),
    (6, 'Maritime escape',         'Discover Halifax with new fares from Toronto.',                                strftime('%Y-%m', 'now', 'localtime', 'start of month', '+1 months'), 'SCHEDULED', strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-7 days')),
    (7, 'Fall foliage sale',       'Autumn fares across Eastern Canada.',                                          strftime('%Y-%m', 'now', 'localtime'), 'ARCHIVED',  strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime', '-40 days'));
