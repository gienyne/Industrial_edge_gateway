# Sensor Connector

## Purpose

The `SensorConnector` is a source-device component responsible for
aggregating measurements from the sensors connected to the ESP32.

It collects the source-specific `SensorReading` objects produced by the
sensor layer and prepares the collected data for transmission to the
Industrial Edge Gateway.

The `SensorConnector` belongs entirely to the source device. It is not a
Gateway-side connector and does not implement the Gateway's `IConnector`
interface.

---

## Responsibilities

The `SensorConnector`

- initializes all configured sensors;
- collects `SensorReading` objects;
- validates acquired readings;
- aggregates the available sensor data;
- prepares the source data for the raw transport layer;
- reports invalid readings and initialization failures for diagnostics.

The `SensorConnector` never

- creates Gateway `Metric` objects;
- creates Gateway `DeviceData` objects;
- encodes Sparkplug B messages;
- publishes Sparkplug messages;
- communicates with the central MQTT publisher;
- accesses databases;
- performs visualization.

The conversion from source-specific data into the Gateway's internal
`Metric` and `DeviceData` models is performed by the corresponding
connector on the Industrial Edge Gateway.

---

## Position in the Architecture

```text
                    ESP32 / Source Device
                           │
                           │
                    ┌──────▼──────┐
                    │ Sensor Layer│
                    └──────┬──────┘
                           │
                           ▼
                    SensorConnector
                           │
                           ▼
                    Source Data
                           │
                           ▼
                     Raw Transport
                           │
                           │
═══════════════════════════╪════════════════════════════════
                           │
                           ▼
                Industrial Edge Gateway
                           │
                    ESP32Connector
                           │
                           ▼
                       Metric
                           │
                           ▼
                     DeviceData
```

The exact raw transport protocol between the ESP32 and the Gateway is
intentionally not defined by this component.

---

## Public Interface

`SensorConnector` is a concrete class because there is currently no need
for multiple implementations of a connector at the source-device level.

```cpp
class SensorConnector
{
public:

    SensorConnector(const SensorArray& sensors);

    bool initialize();

    SourceData collectData();

    const char* name() const;

private:

    SensorArray sensors_;

    void logDiagnostic(const char* message);
};
```

The exact definition of `SourceData` depends on the transport and
serialization design selected for the source device.

---

## Initialization

The connector attempts to initialize every injected sensor.

If one or more sensors fail to initialize, `initialize()` reports the
failure through the diagnostic mechanism.

The connector remains operational and continues to collect data from
successfully initialized sensors.

Sensors that cannot provide a valid measurement produce invalid
`SensorReading` objects, which are excluded from the transmitted source
data.

---

## Data Collection

During each acquisition cycle the connector

- reads every configured sensor;
- receives the corresponding `SensorReading`;
- validates each reading;
- excludes invalid readings;
- aggregates the valid source data;
- forwards the resulting data to the raw transport layer.

Conceptually:

```text
DHT11Sensor ──► DHT11Reading ──┐
ShockSensor ─► ShockReading ───┼──► SensorConnector
LightSensor ─► LightReading ───┘
                                      │
                                      ▼
                                  Source Data
                                      │
                                      ▼
                                 Raw Transport
```

The connector does not transform the readings into the Gateway's
internal `Metric` representation.

---

## Dependency Injection

The connector does not create sensor objects itself.

All sensors are provided during construction.

```text
              Composition Root
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
     DHT11Sensor ShockSensor LightSensor
          │          │          │
          └──────────┼──────────┘
                     ▼
              SensorConnector
```

This keeps sensor creation separate from sensor aggregation and allows
the connector to operate against the common `ISensor` interface.

The injected sensor objects must remain valid for the lifetime of the
`SensorConnector`.

---

## Separation from Gateway Connectors

The name "Connector" is used at two different architectural levels, but
the responsibilities are different.

**Source Device**

```text
ISensor
   ▲
   │
DHT11Sensor
ShockSensor
LightSensor
   │
   ▼
SensorConnector
```

`SensorConnector` aggregates data locally on the ESP32.

**Industrial Edge Gateway**

```text
IConnector
    ▲
    │
    ├── ESP32Connector
    ├── OPCUAConnector
    ├── ModbusConnector
    └── RESTConnector
```

Gateway-side connectors acquire data from heterogeneous source devices and
convert that data into the Gateway's common internal model.

Therefore:

`SensorConnector` ≠ `IConnector`

The `SensorConnector` does not implement `IConnector`.

---

## Gateway Boundary

The architectural boundary is intentionally placed after the raw
transport:

```text
SOURCE DEVICE                         EDGE GATEWAY

SensorReading
     │
     ▼
SensorConnector
     │
     ▼
Raw Transport
     │
     │
═════╪════════════════════════════════
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

`SensorReading` is source-specific.

`Metric` and `DeviceData` belong to the Gateway's internal data model.

This prevents the ESP32 from depending on the Gateway's internal
representation.

---

## Design Principles

- Single Responsibility Principle
- Dependency Injection
- Programming to interfaces for sensor devices
- Hardware abstraction
- Separation of source and Gateway responsibilities
- No Sparkplug or MQTT protocol knowledge
- No dependency on the Gateway's internal data model

---

## Future Extensions

Additional sensors can be added by implementing `ISensor`.

Examples:

- BME280
- CO₂ Sensor
- Pressure Sensor
- Accelerometer

Additional Gateway-side connectors are independent of the
`SensorConnector`.

Examples:

- ESP32Connector
- OPCUAConnector
- ModbusConnector
- RESTConnector

Adding a new Gateway connector does not require changes to the
`SensorConnector` as long as the source-device transport contract remains
unchanged.
