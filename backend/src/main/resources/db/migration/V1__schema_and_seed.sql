CREATE TABLE listings (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT NOT NULL,
    price_per_night DECIMAL(10, 2) NOT NULL,
    max_guests INT NOT NULL,
    location_lat DECIMAL(9,6) NOT NULL,
    location_lon DECIMAL(9,6) NOT NULL,
    amenities JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE availability (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id),
    date DATE NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(listing_id, date)
);

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    guest_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL REFERENCES listings(id),
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE,
    total_price DECIMAL(10, 2) NOT NULL
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    gateway_ref VARCHAR(255),
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    captured_at TIMESTAMP
);

-- Seed Listings (1000 records)
INSERT INTO listings (host_id, price_per_night, max_guests, location_lat, location_lon, amenities, status)
SELECT 
    (random() * 100 + 1)::BIGINT, 
    (random() * 500 + 50)::DECIMAL(10, 2), 
    (random() * 8 + 1)::INT, 
    34.0522 + (random() * 0.1 - 0.05),
    -118.2437 + (random() * 0.1 - 0.05),
    '["wi-fi", "kitchen", "pool"]'::JSONB,
    'ACTIVE'
FROM generate_series(1, 1000);

-- Seed Availability (1000 rows minimum - 10 days for first 100 listings)
INSERT INTO availability (listing_id, date, is_blocked)
SELECT 
    l.id,
    CURRENT_DATE + seq.day,
    random() < 0.2 -- 20% initially blocked
FROM listings l
CROSS JOIN generate_series(1, 10) seq(day)
WHERE l.id <= 100;

-- Seed Bookings (1000 records)
INSERT INTO bookings (guest_id, listing_id, check_in, check_out, status, idempotency_key, total_price)
SELECT 
    (random() * 1000 + 1)::BIGINT,
    l.id,
    CURRENT_DATE - (random() * 30 + 5)::INT,
    CURRENT_DATE - (random() * 5)::INT,
    'COMPLETED',
    md5(random()::text),
    (l.price_per_night * 5)
FROM listings l
WHERE l.id <= 1000;

-- Seed Payments (1000 records corresponding to bookings)
INSERT INTO payments (booking_id, gateway_ref, amount, status, captured_at)
SELECT 
    b.id,
    'stripe_ch_' || substring(md5(random()::text) from 1 for 10),
    b.total_price,
    'CAPTURED',
    CURRENT_TIMESTAMP - interval '5 days'
FROM bookings b;
