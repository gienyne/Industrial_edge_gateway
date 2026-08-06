# Sensor Connector

## Purpose

The `SensorConnector` collects measurements from all configured sensors and
converts them into the common internal data model used by the gateway.

It is the only component that knows which physical sensors are connected.

---

## Responsibilities

The `SensorConnector`

- initializes all sensors
- collects sensor readings
- validates acquired data
- converts valid readings into `Metric` objects
- groups metrics into one `DeviceData`
- reports invalid readings for diagnostic purposes

The `SensorConnector` never

- communicates directly with MQTT
- encodes Sparkplug messages
- accesses databases
- performs visualization

---

## Position in the Architecture

```text
                 Sensor Layer
                       │
                       ▼

                SensorConnector
                       │
                       ▼

                  DeviceData
                       │
                       ▼

               SparkplugEncoder
```

---

## Public Interface

```cpp
class IConnector
{
public:

    virtual bool initialize() = 0;

    virtual DeviceData collectData() = 0;

    virtual const char* name() const = 0;

    virtual ~IConnector() = default;
};
```

---

## SensorConnector

```cpp
class SensorConnector : public IConnector
{
public:

    SensorConnector(Configuration& configuration, const SensorArray& sensors);

    bool initialize() override;

    DeviceData collectData() override;

    const char* name() const override;

private:

    Configuration& configuration_;

    SensorArray sensors_;

    void logDiagnostic(const char* message);

    Metric createMetric( const SensorReading& reading);
};
```

---

## Initialization

The connector attempts to initialize every injected sensor.

If one or more sensors fail to initialize,
`initialize()` returns `false`.

The connector nevertheless remains operational.

Sensors that failed initialization later produce invalid
`SensorReading` objects, which are excluded during data collection.

Initialization failures are reported through
`logDiagnostic()`.

---

## Data Collection

During each acquisition cycle the connector

1. reads every sensor;
2. validates each `SensorReading`;
3. converts valid readings into `Metric` objects;
4. groups all metrics into one `DeviceData`;
5. returns the completed `DeviceData`.

Invalid `SensorReading` objects are excluded from the resulting
`DeviceData`.

The omission is transparent to the remaining gateway components.

Diagnostic information is reported through
`logDiagnostic()`.

---

## Dependency Injection

The connector does not create sensor objects.

All sensors are injected through the constructor.

```text
GatewayApplication
        │
        ▼

 DHT11Sensor
 ShockSensor
 LightSensor
        │
        ▼

 SensorConnector
```

`GatewayApplication` acts as the composition root of the system.

It creates the application components and injects their dependencies
during startup.

The connector assumes that all injected sensor objects remain valid
during its lifetime.

---

## Design Principles

- Single Responsibility Principle
- Dependency Injection
- Programming to interfaces
- Hardware abstraction
- Common internal data model

---

## Future Extensions

Additional connectors can be introduced without modifying the gateway
core.

Examples

- OPCUAConnector
- ModbusConnector
- RESTConnector

Each connector produces the same `DeviceData` representation.