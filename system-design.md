## Overview

Focused deep-dive on the three core features of an Airbnb-style room booking platform: **room search**, **view listing detail**, and **booking**. Each section covers the HLD flow, LLD internals, and tech stack trade-offs.

---

## 1. Room search

### HLD flow

The search path is read-heavy and latency-sensitive. It runs against a dedicated Elasticsearch index — not the primary Postgres DB — keeping queries fast and the main DB unloaded.

```
Guest client
  → API Gateway (auth, rate-limit)
      → Redis cache (popular query results, 60s TTL)
          [cache miss]
      → Search service (filter, rank, paginate)
          → Elasticsearch (geo_distance, full-text, filters)
          → Availability DB (Postgres: cross-check blocked dates)
  ← Results (listings + prices)

Write path (async): listing created/updated → Kafka event → Elasticsearch indexer syncs index
```

### LLD internals

Incoming request carries `{ location, check_in, check_out, guests, filters[], page, sort }`.

1. Hash request into a cache key → check Redis (60s TTL)
2. On cache miss, build Elasticsearch `bool` query:
    - `geo_distance` filter on coordinates
    - `range` on price
    - `terms` on amenities
    - `range` on `max_guests`
3. ES returns ranked listing IDs
4. Cross-check IDs against `availability` table in Postgres with a single `NOT EXISTS` subquery (exclude blocked/booked dates in range)
5. Write final result set back to Redis → return to client

### Tech stack trade-offs

| Concern | Option A | Option B | Trade-off |
| --- | --- | --- | --- |
| Search engine | **Elasticsearch** ✓ | PostgreSQL full-text + PostGIS | ES scales horizontally with richer geo/facet support; Postgres is simpler to operate but struggles at millions of listings |
| Geo indexing | **`geo_point` in ES** ✓ | PostGIS `ST_DWithin` | ES geo queries are fast and distributed; PostGIS is more precise for complex polygons (neighborhood boundaries) |
| Cache layer | **Redis (60s TTL)** ✓ | No cache | Caching popular city searches saves ~80% of ES load; staleness risk is low for search results |
| Availability filter | **Postgres cross-check** ✓ | Denormalized in ES | Postgres is always accurate; denormalizing into ES risks stale data causing double-bookings |

> **Key rule:** Never denormalize availability into Elasticsearch. Use ES for geo + text ranking, then Postgres for the final availability filter.
> 

---

## 2. View listing detail

### HLD flow

The detail page needs fresher data than search results — especially price, availability calendar, and reviews. The listing service acts as a BFF (Backend-for-Frontend), making one call that fans out internally.

```
Guest client
  → API Gateway
      → Listing service (BFF — parallel fetch)
          ├── Redis cache (listing snapshot, 5m TTL) → Postgres fallback
          ├── Availability DB (Postgres: blocked dates, next 90 days)
          └── Review service (avg rating + recent 5)
  ← Single assembled response (listing + calendar + reviews + signed CDN photo URLs)
```

### LLD internals

Request: `GET /listings/{id}?check_in=...&check_out=...`

1. Listing service fires three requests **in parallel** (`Promise.all` / goroutines):
    - Fetch listing snapshot from Redis (5min TTL, cache-aside; falls back to Postgres on miss)
    - Query `availability` rows for next 90 days to render the calendar
    - Call review service for aggregate rating + 5 most recent reviews
2. Images are **never proxied** — response returns signed CDN URLs; client loads photos directly from CDN without hitting any origin server
3. Assemble and return one response to client (1 HTTP round-trip)

### Tech stack trade-offs

| Concern | Option A | Option B | Trade-off |
| --- | --- | --- | --- |
| Listing cache | **Redis (5m TTL, cache-aside)** ✓ | CDN edge caching | Redis respects auth context; CDN is faster globally but listing data changes frequently (price, status) |
| Availability calendar | **Query Postgres directly** ✓ | Pre-computed in Redis | Direct Postgres is always fresh; pre-computed Redis is faster but risks showing stale blocked dates |
| Aggregation pattern | **BFF in listing service** ✓ | Client fetches 3 APIs | BFF gives one fast round-trip; client-side fan-out creates waterfall risk and exposes internal service URLs |
| Photo delivery | **S3 signed URLs in response** ✓ | Stream through backend | Signed URLs offload all bandwidth to CDN; streaming is simpler for auth but expensive at scale |

---

## 3. Booking

### HLD flow

Booking has the most critical correctness requirements: no double-bookings, idempotent retries, and atomic payment + confirmation.

```
Client → Gateway → Booking service → Payment service → Kafka / Notif service
```

### LLD sequence

1. `POST /bookings` with `Idempotency-Key` header
2. Gateway validates JWT, routes to booking service
3. **Check idempotency key in Redis** → return cached response if key already exists (handles retries safely)
4. **`SELECT FOR UPDATE`** on `availability` rows for the requested dates (pessimistic lock — blocks concurrent requests racing for same dates)
5. Dates already blocked? → `409 Conflict`
6. `INSERT booking (status = pending)` + mark availability rows as blocked — all in one transaction
7. Call payment service: `authorize(amount, card_token)` → returns `auth_ref`
8. `UPDATE booking (status = confirmed)` + store idempotency key in Redis (24h TTL)
9. Publish `booking.confirmed` event to Kafka
10. Notification service consumes event → sends email + push (async, non-blocking)
11. Return `200 OK { booking_id, status }` to client

**Payment failure path:** `UPDATE booking (status = cancelled)` + release availability lock → return `402 Payment Required`

**Retry safety:** Client retries with same `Idempotency-Key` → booking service returns cached `200` without re-processing

### Booking state machine

```
pending → confirmed → checked_in → completed
       ↘ cancelled
```


### Critical correctness guarantees

- **No double-booking:** `SELECT FOR UPDATE` on `availability` rows inside a transaction — second concurrent request waits for the lock, then sees already-blocked rows → `409`
- **No duplicate charges:** Idempotency key stored in Postgres; retries always return cached response
- **No stuck pending bookings:** Background job scans for `status = pending` bookings older than 10 minutes → cancels them and releases the availability lock

---

## Data model (key tables)

| Table | Key columns |
| --- | --- |
| `listings` | `id`, `host_id`, `price_per_night`, `max_guests`, `location (geo)`, `amenities (jsonb)`, `status` |
| `availability` | `id`, `listing_id`, `date`, `is_blocked` — one row per night |
| `bookings` | `id`, `guest_id`, `listing_id`, `check_in`, `check_out`, `status (enum)`, `idempotency_key`, `total_price` |
| `payments` | `id`, `booking_id`, `gateway_ref`, `amount`, `status (enum)`, `captured_at` |

> **Availability as individual date rows (not ranges):** Storing one row per night makes blocking, querying, and unlocking individual nights simple with index-only scans. Date ranges require splitting/merging logic and are harder to lock atomically.
>