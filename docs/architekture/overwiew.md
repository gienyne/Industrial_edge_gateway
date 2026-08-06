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

* Single Responsibility Principle
* Dependency Injection
* Programming to Interfaces
* Modular Architecture
* Common Internal Data Model
* Protocol Independence
* Hardware Independence
* Extensibility without modifying existing components

---

## Data Flow

The following diagram illustrates the flow of data through the gateway.

It represents the **data flow** only.

The ownership and composition of the application are described separately in
`gateway-application.md`.

```text
                   +----------------------+
                   |    Configuration     |
                   +----------------------+
                            │
                            ▼

                   +----------------------+
                   |    Sensor Layer      |
                   +----------------------+
                            │
                            ▼

                   +----------------------+
                   |  Sensor Connector    |
                   +----------------------+
                            │
                            ▼

                   +----------------------+
                   | Internal Data Model  |
                   +----------------------+
                            │
                            ▼

                   +----------------------+
                   | Sparkplug Encoder    |
                   +----------------------+
                            │
                            ▼

                   +----------------------+
                   |   MQTT Publisher     |
                   +----------------------+
                            │
                            ▼

                      MQTT Broker
                            │
                            ▼

                      MQTT Clients
```

Every measurement follows the same processing pipeline.

```text
Physical Sensor
        │
        ▼

SensorReading
        │
        ▼

DeviceData
        │
        ▼

SparkplugPayload
        │
        ▼

MQTT Broker
```

Each architectural layer transforms the data into the representation required
by the next layer while remaining independent of all other layers.

---

## Documentation Structure

| Document                 | Description                           |
| ------------------------ | ------------------------------------- |
| `data-models.md`         | Common internal data model            |
| `configuration.md`       | Configuration management              |
| `sensor-layer.md`        | Sensor abstraction                    |
| `sensor-connector.md`    | Data acquisition and aggregation      |
| `sparkplug-encoder.md`   | Sparkplug B encoding                  |
| `mqtt-publisher.md`      | MQTT transport layer                  |
| `gateway-application.md` | Application composition and lifecycle |
| `adr/`                   | Architecture Decision Records (ADRs)  |

---

## Recommended Reading Order

For readers discovering the project for the first time, the following order is
recommended.

1. `overview.md`
2. `data-models.md`
3. `configuration.md`
4. `sensor-layer.md`
5. `sensor-connector.md`
6. `sparkplug-encoder.md`
7. `mqtt-publisher.md`
8. `gateway-application.md`
9. `adr/`

---

## Architectural Evolution

The architecture is designed to evolve by introducing new components rather
than modifying existing ones.

Examples include

* additional sensor implementations
* new connector types (OPC UA, Modbus..)
* new communication protocols
* additional storage backends
* monitoring and diagnostics services

The common internal data model remains the stable contract between all
components, allowing the gateway to evolve without affecting the overall
architecture.
