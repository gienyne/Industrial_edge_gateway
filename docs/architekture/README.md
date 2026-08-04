# Gateway Architecture

## Overview

The Industrial Edge Gateway follows a modular architecture.

Each component has a single responsibility and communicates through a
common internal data model.

The architecture is designed to evolve from simple ESP32 sensors to
industrial machines without requiring changes to the gateway core.

---

## Design Principles

- Single Responsibility Principle
- Modular design
- Easy extensibility
- Common internal data model
- Protocol-independent gateway core

---

## Architecture

```text
                     +----------------------+
                     |    Configuration     |
                     +----------------------+
                          │
                          ▼

                 +-----------------------+
                 |   Sensor Connector    |
                 +-----------------------+
                          │
         ┌────────────────┼────────────────┐
         ▼                ▼                ▼

     DHT11Sensor     ShockSensor      LightSensor

                          │
                          ▼

                 +-----------------------+
                 |  Internal Data Model  |
                 +-----------------------+
                          │
                          ▼

                 +-----------------------+
                 |  Sparkplug Encoder    |
                 +-----------------------+
                          │
                          ▼

                 +-----------------------+
                 |    MQTT Publisher     |
                 +-----------------------+
                          │
                          ▼

                     MQTT Broker
                          │
                          ▼

                    MQTT Explorer
```

---

## Component Responsibilities

### Configuration

Provides the application configuration.

Examples

- MQTT Broker
- MQTT Port
- Group ID
- Edge Node ID
- Publish Interval

---

### Sensor Connector

Collects measurements from connected sensors and converts them into the
common internal data model.

Current implementation

- DHT11 Sensor
- Shock Sensor
- Light Sensor

Future implementations

- OPC UA Connector
- Modbus Connector
- REST Connector

---

### Internal Data Model

Represents every connected device using a common structure that is
independent of the underlying communication protocol.

Example

```cpp
DeviceData
{
    deviceId;

    std::vector<Metric> metrics;
}

Metric
{
    name;
    datatype;
    value;
    timestamp;
}
```

The gateway core operates exclusively on this representation.

---

### Sparkplug Encoder

Converts the internal data model into valid Sparkplug B payloads.

The encoder is independent of how the data were acquired.

---

### MQTT Publisher

Publishes Sparkplug B messages to the configured MQTT Broker.

---

## Future Evolution

When new data sources are added, only the connector layer changes.

```text
OPCUAConnector
ModbusConnector
RESTConnector
        │
        ▼
Internal Data Model
        │
        ▼
Sparkplug Encoder
        │
        ▼
MQTT Publisher
```

The remaining architecture stays unchanged.
