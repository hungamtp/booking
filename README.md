# Airbnb Booking Platform

A high-traffic Airbnb-style room booking platform built with a **Java Spring Boot Modular Monolith** backend and a **Next.js App Router** frontend.

## Architecture Highlights
- **Backend (Spring Boot 3 / Java 21)**: 
  - `Listing`, `Availability`, `Booking`, and `Payment` core entities efficiently mapped to **PostgreSQL**.
  - Automatic migration and seeding of 1000 listings natively through **Flyway**.
  - A Backend-For-Frontend (BFF) Layer executing high-concurrency requests across Postgres, Redis caches, and Elasticsearch.
  - Bullet-proof **Pessimistic Locking** (`SELECT FOR UPDATE`) injected into the checkout sequence to mathematically eliminate double-bookings.
  - Safe retry processing enforcing **idempotency** leveraging Redis keys.
- **Frontend (Next.js 15)**: 
  - **TailwindCSS** responsive, modern UI implementation.
  - Search page fully utilizing React hooks and layout states.
  - Highly optimized Next.js App Router providing Server-Side Rendering capabilities (`standalone` build format).
  - Secure booking checkout flows securely proxying standard API pathways directly into JVM.
- **Infrastructure**: Zero-configuration native infrastructure provided via **Docker Compose**:
  - PostgreSQL 15
  - Redis 7
  - Elasticsearch 8
  - Kafka 3.7

## Prerequisites
- [Docker & Docker Compose](https://www.docker.com/) installed on your machine.

## How to Run Locally

You can boot up the entire stack—databases, message brokers, caching nodes, and the Java/Node architectures—using a single command out of the box:

```bash
docker-compose up -d --build
```

- **Frontend Application Interface**: [http://localhost:3000](http://localhost:3000)
- **Backend JSON API Layer**: [http://localhost:8080](http://localhost:8080)

### Manual Local Development
If you prefer running just the databases via Docker while developing the software stacks locally on your system, execute the targeted dependencies instead:

```bash
# 1. Start specifically just the infrastructure containers
docker-compose up -d postgres redis elasticsearch kafka

# Wait 30-60 seconds for images to pull and fully initialize...

# 2. Start the Spring Boot Backend Terminal Process
cd backend
./gradlew bootRun

# 3. Start the Next.js Frontend Terminal Process (Requires new terminal tab)
cd frontend
npm install
npm run dev
```

Navigate to [http://localhost:3000](http://localhost:3000) when both services are compiling and returning normally!
