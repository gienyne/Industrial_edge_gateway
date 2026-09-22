# Sensor Connector

## Purpose

`SensorConnector` is the source-device component that aggregates the sensors
of one ESP32 into a single `SourceData` structure, ready to be sent to the
gateway.

It belongs entirely to the source device. It is not a gateway-side connector
and does not implement `IConnector`.

Its responsibility ends when the source data is ready for transmission.

---

## Responsibilities

`SensorConnector`

* initializes every configured sensor;
* reads every sensor once per cycle;
* keeps only valid readings;
* groups them into `SourceData`;
* reports sensor failures for diagnostics.

`SensorConnector` never

* creates a `Metric` or a `DeviceData`;
* converts readings into the gateway's internal model;
* encodes or publishes Sparkplug messages;
* talks to the gateway's MQTT publisher.

The conversion into `Metric` and `DeviceData` happens on the gateway side,
in `ESP32Connector`, after the source data has crossed the transport boundary.

---

## Position in the Architecture

The source-device and gateway sides are separated by the raw transport:

```text
        ESP32 / SOURCE DEVICE
        =====================

 DHT11Sensor ──┐
 ShockSensor ──┤
 LightSensor ──┼──► SensorConnector
 ButtonSensor ─┘          │
                          ▼
                     SourceData
                          │
                          ▼
                raw/<deviceId> (MQTT/JSON)

──────────────────── TRANSPORT BOUNDARY ────────────────────

                          │
                          ▼
                   ESP32Connector
                          │
                          ▼
                 Metric ─► DeviceData

                          GATEWAY
```

Everything above the transport boundary belongs to the ESP32 source device.
Everything below it belongs to the gateway application.

The raw transport format is documented in `data_models.md`.

---

## Public Interface

```cpp
constexpr size_t SENSOR_COUNT = /* 2 or 3, depending on the build */;
using SensorArray = std::array<ISensor*, SENSOR_COUNT>;

struct SourceData
{
    std::array<SensorReading, SENSOR_COUNT> readings;
    size_t count;
};

class SensorConnector
{
public:
    explicit SensorConnector(const SensorArray& sensors);

    bool initialize();
    SourceData collectData();
    const char* name() const;
};
```

`SENSOR_COUNT` and the sensors stored in `SensorArray` are selected at build
time in each ESP32's `main.cpp`.

The two currently deployed configurations differ:

* one uses DHT11, shock and light sensors (`SENSOR_COUNT = 3`);
* the other uses DHT11 and a button (`SENSOR_COUNT = 2`).

`SensorConnector` does not depend on these concrete sensor types. It operates
through the `ISensor` interface.

`SourceData` is a fixed-size array plus a count, not a `std::vector`. Only the
first `count` entries are meaningful for a given collection cycle.

---

## Initialization

`initialize()` calls `initialize()` on every configured sensor.

A failed sensor initialization is reported through `logDiagnostic()`, but the
connector remains usable. `collectData()` continues to operate and simply
receives no valid reading from that sensor while the failure persists.

The current sensor implementations always return `true` from `initialize()`,
so this failure path is currently available for diagnostics but is not
triggered in normal operation.

---

## Data Collection

Each collection cycle reads all configured sensors once.

```text
DHT11Sensor  ─► read() ─┐
ShockSensor  ─► read() ─┤
LightSensor  ─► read() ─┼──► SourceData
ButtonSensor ─► read() ─┘
                         │
                         └── only successful readings are kept
```

For every sensor:

1. `SensorConnector` calls `read()`.
2. The sensor fills a `SensorReading`.
3. If `read()` returns `true`, the reading is appended to `SourceData`.
4. If `read()` returns `false`, the reading is discarded and the failure is
   logged.
5. The remaining sensors are still processed.

A failed sensor therefore does not stop the collection cycle.

`SourceData` contains only the readings successfully produced during that
cycle. It remains in the source device's own representation and is not
converted into `Metric` or `DeviceData`.

The resulting `SourceData` is serialized according to the raw transport
format documented in `data_models.md`.

---

## Dependency Injection

The sensors are constructed in `main.cpp` and passed to `SensorConnector`
through a fixed array of `ISensor*`.

```text
main.cpp
   │
   ├── DHT11Sensor dhtSensor(DHT11_PIN);
   ├── ButtonSensor buttonSensor(BUTTONSENSOR_PIN);
   │
   ▼
SensorArray sensors = { &dhtSensor, &buttonSensor };
   │
   ▼
SensorConnector connector(sensors);
```

`SensorConnector` does not own the sensor objects. The sensor instances have
static storage duration in `main.cpp` and must outlive the connector.

This keeps construction of the hardware-specific objects outside the
aggregation logic.

---

## Separation from Gateway Connectors

The project uses the name "connector" for two different components that live
on opposite sides of the transport boundary.

```text
SOURCE DEVICE                              GATEWAY

SensorConnector                            ESP32Connector
      │                                          │
      │ aggregates                               │ converts
      ▼                                          ▼
  SourceData                                   Metric
                                                 │
                                                 ▼
                                             DeviceData
```

`SensorConnector` does not implement `IConnector`.

`ESP32Connector` implements `IConnector` and is responsible for handling the
source-specific transport and converting the received data into the
gateway's internal model.

The two components do not call each other directly. The raw transport
separates them.

---

## Design Principles

* **Single Responsibility.** `SensorConnector` collects and groups sensor readings; it does not convert them into gateway data.
* **Dependency Injection.** Sensors are provided by the source-device composition code.
* **Interface-based Design.** `SensorConnector` works with `ISensor`, not concrete sensor classes.
* **Failure Isolation.** A failed sensor does not prevent the remaining sensors from being read.
* **Clear Boundary.** `SourceData` remains on the source-device side; `Metric` and `DeviceData` belong to the gateway.
* **No Protocol Knowledge.** Sensor collection does not depend on Sparkplug or the gateway's MQTT publisher.

---

## Future Extensions

Adding a sensor to an ESP32 build requires adding it to its `SensorArray` and
adjusting `SENSOR_COUNT`. `SensorConnector` itself does not need to change.

Additional gateway-side connectors such as Modbus or REST are independent of
`SensorConnector`, provided they define their own source-specific input and
conversion path.
