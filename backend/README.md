# Industrial Edge Gateway - Persistence Backend

The `persistence` backend is the Java service responsible for receiving
Sparkplug B messages published by the Gateway over MQTT, decoding their
Protobuf payloads, and persisting the data into TimescaleDB.

This service is part of the `industrial_edge_gateway` project.

## Main Technologies

| Element | Choice |
|---|---|
| Language | Java 21 |
| Build system | Gradle 8.10 |
| Framework | Spring Boot 3.5.6 |
| Database | TimescaleDB on PostgreSQL |
| Data access | Spring Data JPA / Hibernate |
| Database migrations | Flyway |
| Messaging | MQTT with Eclipse Paho |
| Message format | Sparkplug B / Protocol Buffers |


## Ingestion Flow

The ingestion pipeline receives a decoded Sparkplug B `Payload` together with
the parsed `SparkplugTopic`.

For each message:

1. Messages other than device-level `DBIRTH` and `DDATA` are ignored.
2. The device is looked up by its Sparkplug `deviceId`.
   - If the device already exists, its `lastSeenAt` timestamp is updated.
   - If the device does not exist, a new `Device` entity is created with
     `firstSeenAt` and `lastSeenAt` set to the current backend time.
3. The device entity is persisted through `DeviceRepository`.
4. Each metric in the Sparkplug payload is processed:
   - `Node Control/Rebirth` is deliberately ignored and is not stored as a
     measurement.
   - Supported value types (`Boolean`, `Integer`, `Double`, `String`) are
     converted from Sparkplug `Metric` objects into `Measurement` entities.
   - Unsupported Sparkplug value types are logged and skipped.
5. The resulting `Measurement` objects are collected into a `List` and
   persisted through `MeasurementRepository.saveAll(...)`.

### Measurement timestamps

Two different timestamps are used for different purposes:

- `Measurement.time` comes from the timestamp carried by the Sparkplug metric.
  It represents the timestamp associated with the measurement itself.
- `Device.lastSeenAt` uses the backend's current time (`Instant.now()`).
  It represents when the backend actually processed a message from that device.

These values must not be conflated: a measurement may have been acquired
before the backend receives it.