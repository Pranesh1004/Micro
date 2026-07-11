# ARFOMS Microservices Split

This folder contains a Spring Boot microservices version of the airline management system, split by business module:

- `config-server` (centralized configuration management)
- `discovery-server` (service registry via Eureka)
- `auth-service` (JWT token generation and authentication)
- `api-gateway` (single ingress for all module APIs)
- `flight-service` (schedule and fare)
- `booking-service` (booking and seat inventory)
- `checkin-service` (check-in and boarding)
- `crew-service` (crew roster)
- `loyalty-service` (frequent flyer)

## Port configuration

- Config Server: `8888`
- Discovery Server (Eureka): `8761`
- Auth Service: `8180`
- API Gateway: `8210`
- Flight: `8181`
- Booking: `8182`
- CheckIn: `8193`
- Crew: `8184`
- Loyalty: `8185`
- Frontend static server: `5600`

## MySQL configuration

All services are configured for:

- Schema: `microAir_db`
- Username: `root`
- Password: `root`

SQL bootstrap script:

- `microservices/sql/microAir_db_schema.sql`

## Local run

For local development, it is **critical** to start the services in the correct order to avoid connection errors (`TransportException`). 

1. **Config Server First**: Start `config-server` and wait for it to run on `8888`.
2. **Discovery Server Second**: Start `discovery-server` and verify it binds to `8761`.
3. **Other Services**: Start `api-gateway`, `auth-service`, and all other microservices.

### Build and Run using the helper script

The easiest way to build and run everything in the correct order is:

```bash
cd microservices
mvn clean package -DskipTests
./start-services.sh
```

## API Gateway paths

Use gateway base URL `http://localhost:8210` and call existing APIs unchanged:

- `http://localhost:8210/api/flights/...`
- `http://localhost:8210/api/bookings/...`
- `http://localhost:8210/api/checkin/...`
- `http://localhost:8210/api/crew/...`
- `http://localhost:8210/api/loyalty/...`

## Frontend integration status

The existing frontend pages are wired through the gateway (`http://localhost:8210`) and can be served locally from `http://localhost:5600`:

- Reservation flow: `search-flights.html` -> `passenger-details.html` -> `seatInventory.html` -> `payment.html` -> `booking-confirmation.html`
- Manage bookings: `manage-booking.html`
- Flight scheduler: `flights.html`
- Crew operations: `crew_roster_management.html`
- Loyalty portals: `loyalty_admin_portal.html`, `loyalty_agent_portal.html`
- Check-In module: `checkin-operations.html` (linked from `Ground Staff` role in `login.html`)

## Integration behavior

- `booking-service` validates `flightId` via `flight-service` before confirming booking.
- `checkin-service` validates booking existence/status via `booking-service` before check-in.


