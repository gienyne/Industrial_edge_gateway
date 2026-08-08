# Gateway Connectors

## Purpose

Gateway connectors integrate external source devices and industrial systems
into the Industrial Edge Gateway.

Each connector is responsible for acquiring data from one source and
converting it into the common internal data model.

The gateway core does not depend on the specific source technology.

---

## Responsibilities

A gateway connector

- communicates with its source system;
- acquires source data;
- converts source-specific data into `Metric` objects;
- groups the metrics into a `DeviceData` object;
- provides the device identity associated with the source;
- reports acquisition or communication errors.

A connector never

- encodes Sparkplug messages;
- publishes MQTT messages;
- merges data from different connectors;
- contains visualization logic.

Each connector produces its own `DeviceData`.

---

## Position in the Architecture

```text
Source Devices / Industrial Systems
                │
                │ source-specific communication
                ▼
        Gateway Connectors
                │
                │ common internal data model
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
```

Gateway connectors form the boundary between source-specific
communication and the gateway's common internal data model.

---

## IConnector Interface

All gateway connectors implement the common `IConnector` interface.

```cpp
class IConnector
{
public:

    virtual ~IConnector() = default;

    virtual bool initialize() = 0;

    virtual DeviceData collectData() = 0;

    virtual const char* name() const = 0;
};
```

The interface allows `GatewayApplication` to handle different connector
types through a common abstraction.

---

## Interface Semantics

### `initialize()`

Initializes the connection and resources required by the connector.

Returns `true` if initialization succeeds.

A return value of `false` indicates that the connector could not be
initialized successfully.

### `collectData()`

Acquires the current data from the source and returns it as a `DeviceData`
object.

Each invocation represents one acquisition cycle for that connector.

The returned `DeviceData` contains only the metrics belonging to that
connector's source device.

### `name()`

Returns a human-readable identifier for the connector implementation.

It can be used for diagnostics, logging and monitoring.

---

## Current and Planned Implementations

### ESP32Connector

Connects the Industrial Edge Gateway to an ESP32-based source device.

The ESP32 performs local sensor acquisition and transmits source data to
the gateway through a raw transport mechanism.

The exact raw transport protocol remains independent from the
`IConnector` interface.

Conceptually:

```text
ESP32
 │
 │ Raw Transport
 ▼
ESP32Connector
 │
 ▼
DeviceData
```

### OPCUAConnector

Future connector for acquiring data from OPC UA-enabled industrial
equipment.

```text
OPC UA Device
     │
     ▼
OPCUAConnector
     │
     ▼
DeviceData
```

### ModbusConnector

Future connector for integrating Modbus-enabled devices.

```text
Modbus Device
     │
     ▼
ModbusConnector
     │
     ▼
DeviceData
```

### RESTConnector

Future connector for integrating devices or systems that expose a REST
interface.

```text
REST API
   │
   ▼
RESTConnector
   │
   ▼
DeviceData
```

---

## Connector Configuration

Each connector has its own configuration structure and, through it, its own
device identity. Connector-specific configuration — including the
`deviceId` — is not stored in the global `Configuration` object, because the
gateway may handle multiple source devices.

For example:

```cpp
struct ESP32ConnectorConfig
{
    const char* deviceId;

    // Future transport-specific parameters
};
```

The configuration is injected when the connector is constructed:

```cpp
class ESP32Connector : public IConnector
{
public:

    explicit ESP32Connector(const ESP32ConnectorConfig& config);

    bool initialize() override;

    DeviceData collectData() override;

    const char* name() const override;
};
```

The same principle applies to other connector types, each with its own
configuration structure:

```text
ESP32ConnectorConfig
        ├── deviceId
        └── transport-specific parameters

OPCUAConnectorConfig
        ├── deviceId
        └── OPC UA parameters
```

Examples:

```text
ESP32Connector  → deviceId = ESP32-01
OPCUAConnector  → deviceId = PLC-01
ModbusConnector → deviceId = Motor-01
```

The mechanism used to populate these configuration structures is
intentionally independent from the connector interface. Values may
initially be provided directly by `GatewayApplication` and may later
originate from an external configuration source, without affecting
`IConnector` or the common internal data model.

---

## Dependency Injection

`GatewayApplication` acts as the composition root and creates the
connectors together with their dependencies.

Conceptually:

```text
GatewayApplication
        │
        ├── ESP32ConnectorConfig
        │          │
        │          ▼
        │    ESP32Connector
        │
        ├── OPCUAConnectorConfig
        │          │
        │          ▼
        │    OPCUAConnector
        │
        └── ModbusConnectorConfig
                   │
                   ▼
             ModbusConnector
```

The connectors are stored and accessed through the `IConnector` interface.

This allows `GatewayApplication` to work with different connector
implementations without depending on their concrete acquisition logic.

---

## Multiple Connectors

The gateway can contain multiple connectors at the same time.

Each connector represents its own source device and produces its own
`DeviceData`.

```text
ESP32Connector
      │
      ▼
DeviceData(<device-id>)
      │
      ▼
SparkplugEncoder
      │
      ▼
MQTTPublisher


OPCUAConnector
      │
      ▼
DeviceData(<device-id>)
      │
      ▼
SparkplugEncoder
      │
      ▼
MQTTPublisher
```

The `DeviceData` objects are processed independently.

Connectors never merge their data into a shared `DeviceData` object.

This allows heterogeneous source devices to be integrated without
changing the common gateway processing pipeline.

---

## Separation from Source-Side Components

`IConnector` and `SensorConnector` belong to different architectural
layers.

`SensorConnector` is a source-side component responsible for aggregating
local sensors on a source device.

`IConnector` represents the gateway-side abstraction used to integrate
that source device into the Industrial Edge Gateway.

```text
SOURCE DEVICE
────────────────────────────────

DHT11Sensor
ShockSensor
LightSensor
      │
      ▼
SensorConnector
      │
      │ Raw Transport
      ▼

════════ GATEWAY BOUNDARY ════════

ESP32Connector
      │
      ▼
DeviceData
```

The gateway does not access the physical sensors directly.

---

## Design Principles

- Single Responsibility Principle
- Dependency Injection
- Programming to Interfaces
- Hardware independence
- Protocol independence
- Explicit configuration ownership
- Common internal data model
- Independent connector data processing

---

## Future Extensions

Additional connector implementations can be introduced without modifying
the gateway core.

Possible extensions include

- OPC UA
- Modbus
- REST
- other industrial communication protocols;
- additional source-device types.

The common `IConnector` interface and `DeviceData` representation remain
the stable integration boundary.

Future configuration sources may also be introduced without changing the
connector interface.

For example:

```text
External Configuration
        │
        ▼
ConnectorConfig
        │
        ▼
IConnector
        │
        ▼
DeviceData
```