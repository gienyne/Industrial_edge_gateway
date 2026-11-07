# Sprint 3

## Objective

Persist the standardized data received from the Industrial Edge Gateway without coupling the gateway to a database.

A separate backend service subscribes to the Sparkplug B stream, decodes incoming messages and stores device information and measurements in a time-series database.

The gateway remains unchanged and independent of the persistence layer.

---

## Sprint 3 Architecture

```text
Gateway
   │
   │ Sparkplug B / MQTT
   ▼
MQTT Broker
   │
   │ spBv1.0/#
   ▼
┌──────────────────────────────┐
│     Persistence Backend     │
│       Java / Spring Boot     │
│                              │
│    SparkplugMqttListener     │
│              │               │
│              ▼               │
│       IngestionService       │
└──────────────┬───────────────┘
               │ JPA
               ▼
          TimescaleDB
           (PostgreSQL)
```

The backend acts as a Sparkplug Host Application. It consumes the published data and stores it without modifying the gateway.

---

## Planned Tasks

### Backend

- [x] Create Spring Boot project (Java 21, Gradle)
- [x] Generate Java Protobuf classes from `sparkplug_b.proto`
- [x] Connect to the MQTT broker using Eclipse Paho
- [x] Subscribe to `spBv1.0/#`
- [x] Implement Sparkplug topic parsing (`SparkplugTopic`)
- [x] Decode Sparkplug B payloads
- [x] Implement `IngestionService` for device and measurement persistence
- [x] Extract metric units from the Sparkplug `unit` property

### Database

- [x] Select TimescaleDB on PostgreSQL
- [x] Manage the schema using Flyway migrations
- [x] Create `devices` and `measurements` tables
- [x] Configure `measurements` as a time-partitioned hypertable
- [x] Add indexes for device, metric and time
- [x] Enforce typed measurement values with a database constraint

### Integration

- [x] Integrate the persistence backend into `develop`
- [x] Verify that gateway data is received and stored correctly

---

## Data Processing

For each DBIRTH or DDATA message, the backend:

1. Creates or updates the corresponding device and its `lastSeenAt` timestamp.
2. Converts supported metric types (`Boolean`, `Integer`, `Double`, `String`) into measurements.
3. Avoids storing duplicate values when DBIRTH repeats existing values.
4. Persists measurements in batches.

---

## Decisions

- **Persistence architecture:** the backend consumes Sparkplug B instead of having the gateway write directly to the database. This keeps the gateway independent of storage.
- **Database:** TimescaleDB was selected because measurements are time-series data, while retaining PostgreSQL compatibility and standard SQL functionality.
- **Timestamps:** `Measurement.time` represents the timestamp carried by the Sparkplug metric. `Device.lastSeenAt` records when the backend processed the message.
- **Measurement model:** each metric value is stored as one row, with typed value columns. A database constraint ensures that exactly one value column is populated.
- **Schema management:** Flyway manages database migrations. Hibernate uses `ddl-auto: validate` and does not modify the schema.
- **Sprint order:** persistence was moved forward because access to the Smart Factory network and real machines was not yet available. Real machine integration was postponed to Sprint 5.

---

## Sprint 3 Success Criteria

- [x] Receive DBIRTH and DDATA messages from the gateway.
- [x] Persist devices and measurements with their correct types, units and timestamps.
- [x] Create the database schema through Flyway migrations on an empty database.

---

## Next Sprint

Sprint 4 adds device and Edge Node status management, a REST API and a dashboard on top of the persisted data.
