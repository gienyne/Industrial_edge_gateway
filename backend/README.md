# Industrial Edge Gateway - Persistence Backend

Backend service for the `industrial_edge_gateway` project. It consumes Sparkplug B messages over MQTT, tracks edge node and device status, stores measurements in TimescaleDB, records system events, and exposes a REST API for the dashboard.

```text
Gateway ── MQTT / Sparkplug B ──▶ Backend ──▶ TimescaleDB
                                     ▲
                                     │ REST API
                                     ▼
                                 Dashboard
```

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Build | Gradle 8.10 |
| Framework | Spring Boot 3.5.6 |
| Database | TimescaleDB on PostgreSQL |
| Data access | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Messaging | MQTT with Eclipse Paho |
| Message format | Sparkplug B / Protocol Buffers |
| Security | Spring Security with session-based authentication and CSRF protection |

## Getting Started

A PostgreSQL database with TimescaleDB and an MQTT broker must be running before starting the backend.

1. Copy `.env.example` to `.env` and configure the required values.
2. From the backend directory, start the application:

   ```powershell
   .\gradlew.bat bootRun --args="--server.port=8082"
   ```

   Flyway applies pending database migrations at startup. If the `users` table is empty and administrator credentials are configured, the initial dashboard user is created. See [Authentication](#authentication).

3. The REST API is available at the configured server port. With the command above, the base URL is `http://localhost:8082`.

To run the automated tests:

```powershell
.\gradlew.bat test
```

The current test suite runs without requiring a live database or MQTT broker.

## Configuration

Application settings are defined in `src/main/resources/application.yml`. Spring Boot imports the optional `.env` file, which supplies the database connection settings and initial dashboard administrator credentials.

The MQTT and Sparkplug settings are configured in `application.yml`, including:

- **MQTT broker:** `tcp://localhost:1883`
- **Sparkplug group ID:** `SFM`
- **Edge node ID:** `IndustrialEdgeGateway`
- **Synchronization window:** 15 seconds

The HTTP port can be overridden at startup using Spring Boot's `--server.port` argument.

## Message Processing

The backend processes the following Sparkplug B message types:

| Message | Behaviour |
|---|---|
| `NBIRTH` | Marks the edge node as online. |
| `NDEATH` | Marks the edge node as offline and sets its devices offline. |
| `DBIRTH` | Marks a device as online, registers its metrics, and stores its initial values. |
| `DDATA` | Stores new measurements for confirmed online devices. If the device is not confirmed online, the message is discarded and a rebirth is requested. |
| `DDEATH` | Marks the device as offline. |

Status changes and relevant processing conditions are recorded as events, including edge node and device connectivity changes, rebirth requests, and Sparkplug sequence gaps.

When a sequence gap is detected, the backend records a `SEQUENCE_GAP` event and requests a new birth sequence from the edge node.

After an MQTT reconnection, previously online states become `UNKNOWN` during the synchronization window, until new birth messages confirm the current status.

### Measurement Storage

For each `DBIRTH` or `DDATA` message:

1. The device is identified by its Sparkplug device ID. Existing devices have their `lastSeenAt` timestamp updated; unknown devices are created.
2. Supported metrics are converted to measurements with one of four data types: `Boolean`, `Integer`, `Double`, or `String`.
3. `Node Control/Rebirth` and unsupported metric types are skipped.
4. Duplicate measurements from `DBIRTH` are avoided for the same device, metric, and timestamp.
5. Measurements are persisted in batches using `MeasurementRepository.saveAll(...)`.

Two timestamps are maintained:

- **`Measurement.time`** — the timestamp carried by the Sparkplug metric.
- **`Device.lastSeenAt`** — the backend's processing time for the latest message received from the device.

## REST API

All routes require an authenticated session, except for the CSRF, login, and logout endpoints.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/overview` | System health, MQTT and database status, device counts, and recent events. |
| `GET` | `/api/devices` | Device list with status, last contact, and metric count. |
| `GET` | `/api/devices/{deviceId}` | Device details, including the latest value of each metric. |
| `GET` | `/api/devices/{deviceId}/measurements/recent` | The 20 most recent measurements for a device. |
| `GET` | `/api/metrics/history` | Measurement history filtered by `deviceId`, `metricName`, `from`, and `to`. |
| `GET` | `/api/events` | Events ordered newest first, with filters and pagination. |

The events endpoint supports the `type`, `severity`, `deviceId`, `from`, and `to` filters. Pagination uses `page` and `size`, with page sizes from 1 to 100.

### Measurement History

History responses depend on the metric type:

- **Numeric metrics** are aggregated into time buckets, targeting approximately 500 points. Each bucket includes an average, minimum, and maximum.
- **Boolean and text metrics** are represented as state changes, beginning with the state at the start of the requested period.

## Authentication

Authentication uses Spring Security and an HTTP session.

- `GET /api/auth/csrf` provides the CSRF token cookie, `XSRF-TOKEN`.
- `POST /api/auth/login` authenticates the user and creates a session.
- `GET /api/auth/me` returns information about the authenticated user.
- `POST /api/auth/logout` ends the session.
- `POST /api/auth/password` changes the password of the authenticated user.

Requests that modify state must include the CSRF token in the `X-XSRF-TOKEN` header, as required by the backend's security configuration.

Password changes require a password of at least 8 characters and no more than 72 UTF-8 bytes, in accordance with BCrypt's input limit. The new password must differ from the current password.

### Initial Administrator

At startup, if the `users` table is empty and `DASHBOARD_ADMIN_USER` and `DASHBOARD_ADMIN_PASSWORD` are configured, the backend creates the initial dashboard user.

### Adding Users

There is no public sign-up endpoint. Additional users can be created directly in the database using a BCrypt password hash. For example:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (user_id, password_hash)
VALUES ('new-user', crypt('Password!', gen_salt('bf', 10)));
```

The new user can then sign in and change their password under **Settings -> Security** in the dashboard.

## Database

Flyway manages the schema through migrations in `src/main/resources/db/migration`.

The schema includes:

- `devices` -> device identity, status, and last-seen timestamps.
- `device_metrics` -> registered metrics for each device.
- `edge_nodes` -> edge node identity and connectivity status.
- `events` -> connectivity changes and other system events.
- `users` -> dashboard accounts and password hashes.
- `measurements` -> time-series measurements stored in a TimescaleDB hypertable partitioned by `time`.