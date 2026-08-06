# Industrial Edge Gateway Architecture

## Overview

This directory contains the architectural documentation of the Industrial Edge
Gateway.

The gateway follows a layered and modular architecture based on separation of
responsibilities and dependency injection.

Each component has a single responsibility and communicates through a common
internal data model. This architecture allows new hardware, communication
protocols and services to be introduced without modifying the gateway core.

---

## Design Principles

The architecture is based on the following principles.

- Single Responsibility Principle
- Dependency Injection
- Programming to Interfaces
- Common Internal Data Model
- Hardware Independence
- Protocol Independence
- Modular Design
- Extensibility

---

## Data Flow

The following diagram illustrates how measurement data flows through the
gateway.

For the ownership and composition structure, see
`gateway-application.md`.

```text
Sensor Layer
      │
      ▼

SensorConnector
      │
      ▼

DeviceData
(Common Internal Data Model)
      │
      ▼

SparkplugEncoder
      │
      ▼

SparkplugPayload
      │
      ▼

MQTTPublisher
      │
      ▼

MQTT Broker
      │
      ▼

MQTT Clients
```

`Configuration` is a cross-cutting service shared by multiple gateway
components.

Its responsibilities and dependencies are documented in
`configuration.md`.

---

## Documentation Structure

| Document | Description |
|----------|-------------|
| `README.md` | Architecture overview |
| `configuration.md` | Configuration management |
| `sensor-layer.md` | Sensor abstraction |
| `sensor-connector.md` | Data acquisition and aggregation |
| `sparkplug-encoder.md` | Sparkplug B encoding |
| `mqtt-publisher.md` | MQTT transport layer |
| `gateway-application.md` | Application composition and lifecycle |
| `adr/` | Architecture Decision Records |

---

## Recommended Reading Order

For readers discovering the project for the first time, the following order is
recommended.

1. README.md
2. configuration.md
3. sensor-layer.md
4. sensor-connector.md
5. sparkplug-encoder.md
6. mqtt-publisher.md
7. gateway-application.md
8. ADRs

---

## Architectural Evolution

The architecture is designed to evolve by introducing new components rather
than modifying existing ones.

Examples include

- additional sensor implementations
- new connector types (OPC UA, Modbus..)
- new communication protocols
- additional storage backends
- monitoring and diagnostics services

The common internal data model remains the stable contract between all
components, allowing the gateway to evolve without affecting the overall
architecture.
