# Industrial Edge Gateway Architecture

## Overview

This directory contains the architectural documentation of the Industrial Edge
Gateway.

The gateway follows a modular architecture based on separation of
responsibilities, dependency injection and programming to interfaces.

The architecture separates source-device data acquisition from the central
gateway processing pipeline.

Source devices acquire their own data and transmit it through a raw transport
interface. The Industrial Edge Gateway receives this data through dedicated
connectors, converts it into a common internal representation and publishes
the resulting data using standardized communication protocols.

This separation allows different source devices and industrial interfaces to
be integrated without coupling the gateway core to a specific hardware or
communication protocol.

---

## Architectural Boundary

The architecture is divided into two main areas.

```text
+--------------------------------+
| Source Device                  |
|                                |
| Sensor Layer                   |
|       │                        |
|       ▼                        |
| SensorConnector                |
|       │                        |
|       ▼                        |
| Raw Transport                  |
+-------┼------------------------+
        │
        │
        │ Raw Device Data
        ▼
+-----------------------------------------------+
| Industrial Edge Gateway                        |
|                                                 |
| ESP32Connector   OPCUAConnector   Modbus...     |
|        │               │             │         |
|        └───────────────┼─────────────┘         |
|                        ▼                       |
|                     Metric                     |
|                        │                       |
|                        ▼                       |
|                   DeviceData                   |
|                        │                       |
|                        ▼                       |
|              SparkplugEncoder                  |
|                        │                       |
|                        ▼                       |
|                SparkplugPayload                |
|                        │                       |
|                        ▼                       |
|                 MQTTPublisher                  |
+------------------------┼-----------------------+
                         │
                         ▼
                    MQTT Broker
```

The exact raw transport between source devices and the gateway is intentionally
kept independent from the gateway's internal data model.

The concrete transport mechanism will be defined when the corresponding
source connector is implemented.

---

## Source Devices

Source devices are responsible for acquiring data from their local hardware.

For example, an ESP32 may contain several physical sensors.

```text
ESP32
 ├── DHT11Sensor
 ├── ShockSensor
 └── LightSensor
```

The source device converts its hardware-specific measurements into a form
suitable for transmission to the Industrial Edge Gateway.

The source device does not implement the central gateway data processing
pipeline.

In particular, source devices do not need to create the gateway's
`DeviceData` model or encode Sparkplug B messages.

---

## Gateway Connectors

The Industrial Edge Gateway uses connectors to integrate different types of
source devices and industrial interfaces.

Examples include:

- ESP32Connector
- OPCUAConnector
- ModbusConnector
- RESTConnector

Each connector is responsible for understanding its source-specific
communication mechanism and transforming the received data into the
gateway's common internal representation.

```text
Source-specific data
        │
        ▼
    Connector
        │
        ▼
      Metric
        │
        ▼
   DeviceData
```

The gateway core therefore does not need to know whether data originated from
an ESP32, an OPC UA server, a Modbus device or another source.

See `gateway-connectors.md` for the connector interface, its semantics and
the current and planned implementations.

---

## Common Internal Data Model

The gateway uses a common internal data model as the contract between source
integration and gateway processing.

```text
Metric
   │
   ▼
DeviceData
```

`Metric` represents one standardized measurement.

`DeviceData` groups the metrics belonging to one physical device.

Each connector produces its own `DeviceData`.

Different devices are processed independently and are not implicitly merged
into a single device representation.

The ownership of the main data models is defined as follows:

| Model               | Ownership            |
|----------------------|-----------------------|
| `SensorReading`       | Source-specific       |
| `Metric`              | Gateway internal      |
| `DeviceData`          | Gateway internal      |
| `SparkplugPayload`    | Gateway communication |

This separation prevents source-specific models from leaking into the gateway
core.

---

## Sparkplug and MQTT

Sparkplug B is implemented as a communication layer of the Industrial Edge
Gateway.

```text
DeviceData
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
```

The `SparkplugEncoder` is responsible for Sparkplug-specific processing,
including topic construction, payload encoding and sequence management.

The `MQTTPublisher` is responsible only for MQTT transport.

This keeps Sparkplug knowledge separate from the underlying MQTT client
implementation.

---

## Configuration

Configuration is a cross-cutting gateway service shared by components that
require gateway-level settings.

```text
                 Configuration
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
     Connectors   SparkplugEncoder  MQTTPublisher
```

Gateway configuration is independent from the local configuration of source
devices.

Source-specific parameters remain outside the global gateway configuration
model.

See `configuration.md` for the detailed configuration model.

---

## Application Composition

`GatewayApplication` acts as the composition root of the Industrial Edge
Gateway.

It creates and connects the major gateway components through dependency
injection.

Conceptually:

```text
GatewayApplication
        │
        ├── Configuration
        │
        ├── IConnector
        │      ├── ESP32Connector
        │      ├── OPCUAConnector
        │      └── ModbusConnector
        │
        ├── ISparkplugEncoder
        │
        └── IMqttPublisher
```

The application coordinates the lifecycle and execution flow without
implementing source-specific acquisition logic or protocol encoding itself.

See `gateway-application.md` for the detailed composition and lifecycle.

---

## Design Principles

The architecture is based on the following principles.

### Single Responsibility Principle

Each component has one clearly defined responsibility.

### Dependency Injection

Dependencies are explicitly provided by the application composition root.

### Programming to Interfaces

Interfaces are used where concrete implementations need to be replaceable,
for example for connectors, Sparkplug encoding and MQTT transport.

### Common Internal Data Model

Source-specific data is converted into a common gateway representation before
entering the central processing pipeline.

### Hardware Independence

The gateway core does not depend on a specific source device or sensor.

### Protocol Independence

The gateway's internal data model is independent from MQTT, Sparkplug B,
OPC UA, Modbus and other communication protocols.

### Modular Design

Individual components can evolve independently as long as their defined
interfaces remain stable.

### Extensibility

New source connectors and services can be integrated without modifying the
core processing pipeline.

---

## Data Flow

The normal data flow through the architecture is:

```text
Source Device
      │
      │ Raw Transport
      ▼
Source Connector
      │
      ▼
Metric
      │
      ▼
DeviceData
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

For each source device, the corresponding connector produces a separate
`DeviceData` object.

The gateway then processes and publishes that device data independently.

---

## Documentation Structure

| Document                 | Description                              |
|---------------------------|-------------------------------------------|
| `README.md`                | Architecture overview                      |
| `data-models.md`           | Common internal data models                |
| `configuration.md`         | Gateway configuration                      |
| `sensor-layer.md`          | Source-device sensor abstraction           |
| `sensor-connector.md`      | Source-device sensor aggregation           |
| `gateway-connectors.md`    | Gateway connector abstraction and implementations |
| `sparkplug-encoder.md`     | Sparkplug B encoding                       |
| `mqtt-publisher.md`        | MQTT transport layer                       |
| `gateway-application.md`   | Application composition and lifecycle      |
| `adr/`                     | Architecture Decision Records              |

---

## Recommended Reading Order

For readers discovering the project for the first time, the following order is
recommended:

1. `README.md`
2. `data-models.md`
3. `overview.md`
4. `sensor-layer.md`
5. `sensor-connector.md`
6. `gateway-connectors.md`
7. `gateway-application.md`
8. `configuration.md`
9. `sparkplug-encoder.md`
10. `mqtt-publisher.md`
11. `adr/`

---

## Architectural Evolution

The architecture is designed to evolve by introducing new source connectors
and gateway components rather than modifying the central processing pipeline.

Examples include:

- additional source-device connectors;
- OPC UA integration;
- Modbus integration;
- additional industrial communication protocols;
- storage backends;
- monitoring and diagnostics services;
- web-based services.

The common internal data model remains the stable contract between source
integration and gateway processing.

This allows the gateway to evolve without coupling the central architecture
to individual machines or acquisition technologies.
