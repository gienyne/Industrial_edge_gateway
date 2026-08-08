# Data Models

## Overview

The Industrial Edge Gateway is built around a common internal data model.

Source devices may use different hardware, sensors and communication
protocols. Their source-specific data is therefore transformed into a
common representation inside the Industrial Edge Gateway before it is
processed further.

The gateway separates source-specific acquisition from its internal data
model and from the communication protocols used for standardized
publication.

The resulting data flow is:

```text
Source Device
    │
    │ Source-specific data
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
SparkplugPayload
    │
    ▼
MQTT Publisher
    │
    ▼
MQTT Broker
```

For sensor-based sources such as the ESP32, hardware-specific sensor
readings may exist before the data reaches the gateway.

```text
ESP32
    │
    ▼
Sensor
    │
    ▼
SensorReading
    │
    ▼
Source Transport
    │
    ▼
ESP32Connector
    │
    ▼
Metric
    │
    ▼
DeviceData
```

Other industrial sources may provide their data through protocols such as
OPC UA or Modbus and may therefore not require a SensorReading layer.

Each model has one clearly defined responsibility and represents a specific
abstraction level.

---

## Data Models

### Configuration

The `Configuration` model represents runtime parameters required by an
application component.

Configuration is independent from measurement data and does not contain
sensor values, metrics or device snapshots.

Typical gateway configuration parameters include:

- MQTT Broker
- MQTT Port
- MQTT Client ID
- Sparkplug Group ID
- Sparkplug Edge Node ID
- Publish Interval
- MQTT QoS
- MQTT Retain Flag

Source devices such as the ESP32 may have their own local configuration,
for example sensor settings, source transport parameters or local sampling
intervals.

Gateway configuration and source-device configuration are therefore
independent concerns.

The exact separation and ownership of these configuration parameters is
defined by the corresponding application components.

---

### SensorReading

`SensorReading` represents a source-specific measurement produced by a
hardware sensor.

This model is only required for sources that directly acquire physical
measurements through sensors.

Different sensors may define different reading structures because they
measure different physical quantities and may provide different metadata.

Current sensor models include:

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

`SensorReading` is source-specific and does not form part of the common
gateway data model.

For example, an OPC UA connector may receive a value directly from an
industrial device and convert it directly into a `Metric` without creating
a `SensorReading`.

---

### Metric

A `Metric` represents one standardized measurement inside the Industrial
Edge Gateway.

Every measurement is converted into this common representation before it
enters the gateway's common processing pipeline.

The original source may be an embedded sensor, an OPC UA server, a Modbus
device, a REST interface or another industrial data source.

Conceptually:

```text
Metric
├── name
├── datatype
├── value
├── unit
└── timestamp
```

Examples:

```text
Temperature  = 24.8 °C
Humidity     = 56 %
Shock        = true
Light        = 820 lx
MotorSpeed   = 1500 rpm
MotorCurrent = 4.2 A
```

The gateway core processes standardized metrics and does not depend on the
hardware-specific structures used by individual source devices.

---

### DeviceData

`DeviceData` is the common internal gateway representation of one physical
device.

It groups all metrics belonging to the same device into one logical data
object.

Conceptually:

```text
DeviceData
├── deviceId
├── metrics
└── timestamp
```

Example:

```text
ESP32-01
├── Temperature
├── Humidity
├── Shock
└── Light
```

A future industrial device follows the same internal structure regardless
of its native communication protocol.

Example:

```text
Robot-01
├── Temperature
├── Motor Current
├── Speed
└── Alarm
```

`DeviceData` belongs to the internal data model of the Industrial Edge
Gateway.

The source device does not need to know or use this internal representation.
It only provides its source-specific data through its communication
interface.

The `deviceId` identifies the physical source represented by the data.

The `timestamp` of `DeviceData` represents the point at which the gateway
assembled the current device snapshot.

Each individual `Metric` keeps the timestamp of its own measurement.

This distinction allows the gateway to preserve both the acquisition time
of individual measurements and the creation time of the complete device
snapshot.

---

### SparkplugPayload

`SparkplugPayload` represents the communication model produced by the
Sparkplug Encoder.

It contains the information required by the MQTT Publisher to publish a
Sparkplug B message.

Conceptually:

```text
SparkplugPayload
├── topic
├── payload
├── messageType
├── seq
└── bdSeq
```

The Sparkplug Encoder transforms the gateway's internal `DeviceData` into
the Sparkplug-specific representation.

```text
DeviceData
    │
    ▼
Sparkplug Encoder
    │
    ▼
SparkplugPayload
```

The MQTT Publisher works exclusively with `SparkplugPayload`.

It does not interact directly with sensors, source-specific data models or
industrial protocols.

Sparkplug-specific concepts therefore remain outside the common internal
data model.

---

## Data Ownership

The ownership of the different models is intentionally separated.

| Model               | Ownership          |
|---------------------|---------------------|
| `SensorReading`      | Source-specific     |
| `Metric`             | Gateway internal    |
| `DeviceData`         | Gateway internal    |
| `SparkplugPayload`   | Gateway communication |

This boundary is fundamental to the architecture.

```text
SOURCE SIDE
────────────────────────────────────────
Sensor
    │
    ▼
SensorReading
    │
    │ Source transport
    ▼

══════════════ GATEWAY BOUNDARY ══════════════

    │
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
SparkplugPayload
    │
    ▼
MQTT Publisher

──────────────────────────────────────────────
GATEWAY SIDE
```

The gateway therefore does not require source devices to understand its
internal `Metric` or `DeviceData` models.

This allows different types of machines to be integrated without forcing
them to adopt the gateway's internal software structures.

---

## Design Principles

### Separation of Responsibilities

Each model represents one abstraction level.

For a sensor-based source:

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

For an industrial source that provides data directly through a protocol
such as OPC UA or Modbus:

```text
Industrial Device
    │
    ▼
Connector
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

### Hardware Independence

The common gateway data model does not depend on a specific sensor or
hardware platform.

Source-specific acquisition remains isolated within the corresponding
source-side components and connectors.

The gateway core operates on `Metric` and `DeviceData` instead of directly
processing hardware-specific structures.

---

### Protocol Independence

The internal data model does not depend on Sparkplug B, MQTT, OPC UA,
Modbus or any other communication protocol.

Source connectors are responsible for transforming protocol-specific source
data into the gateway's common representation.

The Sparkplug Encoder is responsible for transforming the internal gateway
representation into the Sparkplug-specific communication format.

This keeps protocol-specific processing outside the common internal model.

---

### Centralized Standardization

Sparkplug B standardization is performed centrally by the Industrial Edge
Gateway.

Source devices are not required to implement the Sparkplug data model.

The architecture therefore follows:

```text
Heterogeneous Sources
        │
        ▼
Source Connectors
        │
        ▼
Common Gateway Model
        │
        ▼
Sparkplug Encoder
        │
        ▼
Sparkplug B
```

This centralizes the complexity of standardized industrial communication
and prevents every source device from having to implement the same
Sparkplug logic.

---

### Extensibility

Adding support for a new data source requires a corresponding connector
that transforms the source-specific data into the common gateway model.

Examples include:

```text
ESP32Connector
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

The remaining gateway processing pipeline remains unchanged.

Once the connector produces `Metric` and `DeviceData`, the same Sparkplug
encoding and MQTT publication mechanisms can be used for the new source.

---

## Summary

The internal data model forms the foundation of the Industrial Edge Gateway.

It provides:

- a common representation for measurements from different sources;
- a clear separation between source-specific data and gateway-internal data;
- independence from specific hardware platforms;
- independence from source communication protocols;
- centralized Sparkplug B standardization;
- a modular and extensible foundation for future industrial machine
  integration.

The key architectural boundary is:

```text
Source-specific data
        │
        ▼
Source Connector
        │
════════════════════════════════
      Gateway Boundary
════════════════════════════════
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
MQTT
```

`SensorReading` belongs to the source-specific side, while `Metric`,
`DeviceData` and the subsequent processing models belong to the Industrial
Edge Gateway.
