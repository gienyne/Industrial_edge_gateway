# Sensor Layer

## Purpose

The Sensor Layer provides the hardware abstraction for physical sensors on the
ESP32 source device. Each sensor is responsible only for communicating with its
own hardware and producing a measurement.

The Sensor Layer has no knowledge of the gateway, MQTT, Sparkplug or any other
communication protocol.

It belongs entirely to the source device.

---

## Responsibilities

Each sensor

* initializes its own hardware;
* acquires a measurement;
* reports whether the measurement is valid;
* fills a `SensorReading` with the result;
* exposes its name through the common `ISensor` interface.

A sensor never

* creates a `Metric` or `DeviceData`;
* encodes or publishes Sparkplug messages;
* implements gateway logic;
* communicates with the gateway or MQTT directly.

---

## Position in the Architecture

```text
                    ESP32 / SOURCE DEVICE
                    =====================

    DHT11Sensor    ShockSensor   LightSensor   ButtonSensor
          │             │             │             │
          └─────────────┴──────┬──────┴─────────────┘
                               │
                               ▼
                        SensorConnector
                               │
                               ▼
                         raw/<deviceId>

                    =====================
                            GATEWAY
```

`ISensor` is an interface, not a separate processing stage. `SensorConnector`
uses it to interact with all sensors without depending on their concrete
implementations.

```text
DHT11Sensor ─────┐
ShockSensor ─────┤
LightSensor ─────┼──────► SensorConnector
ButtonSensor ────┘
       ▲
       │ implements
     ISensor
```

The transport used after `SensorConnector`, currently MQTT/JSON on
`raw/<deviceId>`, is outside the responsibility of this layer. Its message
format is documented in `data_models.md`.

---

## Public Interface

```cpp
class ISensor
{
public:
    virtual ~ISensor() = default;

    virtual bool initialize() = 0;
    virtual bool read(SensorReading& reading) = 0;
    virtual const char* name() const = 0;
};
```

`read()` writes the measurement into the caller-provided `SensorReading` and
returns whether the measurement is usable.

There is no separate validity flag in `SensorReading`. The return value of
`read()` determines whether the reading is valid; invalid readings are not
passed on by the caller.

---

## SensorReading

`SensorReading` is the source-device representation used to pass measurements
from the concrete sensors to `SensorConnector`.

The `type` field identifies which member of the union is populated.

```cpp
enum class SensorType { DHT11, SHOCK, LIGHT, BUTTON };

struct DHT11Reading  {
    float temperature;
    float humidity;
    unsigned long timestamp;
};

struct ShockReading  {
    bool detected;
    unsigned long timestamp;
};

struct LightReading  {
    int intensity;
    unsigned long timestamp;
};

struct ButtonReading {
    bool pressed;
    unsigned long timestamp;
};

union SensorReadingData {
    DHT11Reading dht11;
    ShockReading shock;
    LightReading light;
    ButtonReading button;
};

struct SensorReading {
    SensorType type;
    SensorReadingData data;
};
```

Each `timestamp` contains the value returned by `millis()`. It represents the
time elapsed since the ESP32 booted, not an absolute date or Unix timestamp.

The gateway does not use this value as the Sparkplug timestamp. The value is
carried with the source reading because it is the timestamp available from the
firmware. The gateway-side timestamping is described in `data_models.md`.

These structures are specific to the ESP32 firmware and are not part of the
gateway's internal data model. `ESP32Connector` converts the received sensor
data into the gateway's `Metric` and `DeviceData` structures.

---

## Current Implementations

The firmware is used on two ESP32 units with different sensor configurations.
The active sensor set is selected at build time through `SensorArray` and
`SENSOR_COUNT` in `main.cpp`.

Both configurations use the same `ISensor` interface, allowing
`SensorConnector` to handle the concrete sensor types uniformly.

### DHT11Sensor

Reads temperature and humidity from the pin passed to its constructor
(`DHT11_PIN`, GPIO4 by default) using the Adafruit DHT library.

`read()` returns `false` if the library reports `NAN` for either measurement.
Such readings are discarded by the caller.

### ShockSensor

Uses a digital input with an internal pull-up resistor.

`detected` is set to `true` when the input reads `LOW`.

The sensor can occasionally report noisy transitions without a mechanical
shock. No software debouncing is currently implemented.

### LightSensor

Reads a raw ADC value in the range `0–4095` from an analog input.

The value is not calibrated to a physical unit such as lux.

### ButtonSensor

Uses a digital input with an internal pull-up resistor.

`pressed` is set to `true` when the input reads `LOW`.

None of the four current sensor implementations reports a failed hardware
initialization; their `initialize()` methods currently return `true`.

---

## Design Principles

* **Single Responsibility.** A sensor is responsible only for accessing its own hardware and producing a reading.
* **Hardware Abstraction.** `ISensor` hides pin assignments, timing details and the underlying sensor library from `SensorConnector`.
* **Common Interface.** All sensors expose the same initialization, reading and identification operations.
* **No Protocol Knowledge.** Sensors know nothing about MQTT, Sparkplug, OPC UA or other communication protocols.
* **No Gateway Knowledge.** A sensor produces a `SensorReading`; how that reading is transmitted or processed is handled by other layers.

---

## Future Extensions

New sensor types can be added by implementing `ISensor` and extending
`SensorType` and `SensorReadingData`.

Examples include a BME280 or a CO₂ sensor.

Such an extension remains within the source-device layer and does not require
changes to the gateway's `Metric` or `DeviceData` model.
