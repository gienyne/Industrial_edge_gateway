# Gateway Architecture

## Overview

The Industrial Edge Gateway follows a modular architecture.

Each software component has a single responsibility.

The architecture is designed to evolve from simple sensors connected to an
ESP32 to industrial machines without requiring major modifications.

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
                         │      │      │
                         │      │      │
                         ▼      ▼      ▼

                 +-----------------------+
                 |   Sensor Connector    |
                 +-----------------------+
                         │
         ┌───────────────┼────────────────┐
         ▼               ▼                ▼

   DHT11Sensor     ShockSensor      LightSensor

                         │
                         ▼

                 +-----------------------+
                 |    Internal Metric    |
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

Provides application settings.

Examples

- MQTT Broker
- MQTT Port
- Group ID
- Edge Node ID
- Publish Interval

---

### Sensor Connector

Collects data from all connected sensors.

Current implementation

- DHT11 Sensor
- Shock Sensor
- Light Sensor

Future implementations

- OPC UA Connector
- Modbus Connector
- REST Connector

---

### Internal Metric

Represents every measurement using a common internal structure.

Example

```cpp
Metric
{
    name;
    datatype;
    value;
    timestamp;
}
```

All remaining software works only with this representation.

---

### Sparkplug Encoder

Converts internal metrics into Sparkplug B payloads.

The encoder is independent of the underlying data source.

---

### MQTT Publisher

Publishes Sparkplug messages to the configured MQTT Broker.

---

## Future Evolution

Only the connector layer changes when new data sources are added.

Example

```text
OPCUAConnector
ModbusConnector
RESTConnector
        │
        ▼
Internal Metric
        │
        ▼
Sparkplug Encoder
        │
        ▼
MQTT Publisher
```

The remaining architecture stays unchanged.