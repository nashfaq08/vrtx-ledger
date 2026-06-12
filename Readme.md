### VRTX Ledger Management System

A ledger-based financial backend built with **Java 17 / Spring Boot 3** and **PostgreSQL**, modelling real-world fintech constraints: double-entry accounting, immutability, idempotency, concurrency safety, and reconciliation.

### Prerequisites
- JDK 17+, Maven 3.9+, Docker (for PostgreSQL and the tests)

### Start the database
```bash
docker compose up -d
```

### Run the application
```bash
mvn spring-boot:run
```
