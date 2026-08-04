# Data Models

## Overview

The Industrial Edge Gateway is built around a common internal data model.

Instead of exchanging hardware-specific or protocol-specific data between
software components, all information is transformed into a common internal
representation before it is processed by the gateway.

This design separates hardware acquisition, business logic and communication
protocols while providing a stable foundation for future extensions.

The complete data flow is shown below.

```text
Sensor
    │
    ▼
SensorReading
    │
    ▼
Metric
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

Each model has exactly one responsibility and represents one abstraction
level of the gateway.

---

# Data Models

## Configuration

The `Configuration` model stores all runtime parameters required by the
application.

It does not contain sensor or machine data.

Typical configuration parameters include

- MQTT Broker
- MQTT Port
- MQTT Client ID
- Sparkplug Group ID
- Sparkplug Edge Node ID
- Publish Interval
- MQTT QoS
- MQTT Retain Flag

The configuration is shared by the gateway components but remains completely
independent from the application data.

---

## SensorReading

`SensorReading` represents the raw output produced by a hardware sensor.

Each sensor defines its own reading model because different sensors measure
different physical quantities.

Current implementation

```text
DHT11Reading
├── temperature
├── humidity
├── timestamp
└── valid
```

```text
ShockReading
├── detected
├── timestamp
└── valid
```

```text
LightReading
├── intensity
├── timestamp
└── valid
```

Keeping hardware-specific readings separated allows every sensor driver to
remain simple and independent from the remaining gateway software.

---

## Metric

A `Metric` represents one standardized measurement inside the gateway.

Every measurement, regardless of whether it originates from an embedded
sensor, an OPC UA server, a Modbus device or another industrial interface,
is converted into this common representation.

Conceptually

```text
Metric
├── name
├── datatype
├── value
├── unit
└── timestamp
```

Examples

```text
Temperature = 24.8 °C

Humidity = 56 %

Shock = true

Light = 820 lx
```

The gateway core processes only metrics and never interacts directly with
hardware-specific structures.

---

## DeviceData

`DeviceData` groups all metrics belonging to one physical device.

Instead of processing isolated measurements, the gateway always processes a
complete device snapshot.

Conceptually

```text
DeviceData
├── deviceId
├── metrics
└── timestamp
```

Example

```text
ESP32

├── Temperature
├── Humidity
├── Shock
└── Light
```

Future industrial devices follow exactly the same structure.

Example

```text
Robot ABB

├── Temperature
├── Motor Current
├── Speed
└── Alarm
```

The `timestamp` of `DeviceData` represents the moment at which the gateway
assembled the complete snapshot of the device.

Each individual `Metric` keeps the timestamp of its own measurement.

This distinction allows the gateway to preserve both the acquisition time
of each measurement and the creation time of the complete device snapshot.

---

## SparkplugPayload

`SparkplugPayload` represents the final message produced by the Sparkplug
Encoder.

It contains all information required for MQTT publication.

Conceptually

```text
SparkplugPayload
├── topic
├── payload
├── messageType
├── seq
└── bdSeq
```

The MQTT Publisher works exclusively with this model and remains completely
independent from sensors, industrial protocols and internal gateway logic.

---

# Design Principles

## Separation of Responsibilities

Each model represents exactly one abstraction level.

```text
Sensor
        │
        ▼
SensorReading
        │
        ▼
Metric
        │
        ▼
DeviceData
        │
        ▼
SparkplugPayload
```

No model combines responsibilities from multiple layers.

---

## Hardware Independence

The gateway core never processes hardware-specific data.

Only the Sensor Connector knows how measurements are acquired.

All remaining components operate exclusively on the common internal data
model.

---

## Protocol Independence

The internal data model does not depend on Sparkplug B, MQTT, OPC UA,
Modbus or any other communication protocol.

Protocol-specific processing is delegated to dedicated components such as
the Sparkplug Encoder or future machine connectors.

---

## Extensibility

Adding support for a new data source requires only a new connector.

Example

```text
OPCUAConnector

ModbusConnector

RESTConnector
        │
        ▼
Metric
        │
        ▼
DeviceData
```

The remaining gateway architecture remains unchanged.

---

# Summary

The internal data model forms the foundation of the Industrial Edge Gateway.

It provides

- a common representation for every measurement;
- clear separation between hardware and communication protocols;
- a modular and extensible architecture;
- a stable software foundation for future industrial machine integration.
