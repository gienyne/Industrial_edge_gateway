# Sensor Layer

## Purpose

The Sensor Layer is the hardware abstraction for the physical sensors on an
ESP32 source device.

Each sensor knows how to communicate with its own hardware and produces a
measurement. It has no knowledge of the gateway, MQTT or Sparkplug.

The Sensor Layer belongs entirely to the source device.

---

## Responsibilities

Each sensor

* initializes its own hardware;
* acquires a measurement;
* reports whether the measurement is valid;
* fills a `SensorReading` with the result;
* exposes its name through the common `ISensor` interface.

A sensor never

* creates a `Metric` or a `DeviceData`;
* creates or publishes MQTT messages;
* encodes Sparkplug messages;
* implements gateway logic.

The separation is intentional:

```text
Sensor
  └── measures physical hardware

SensorConnector
  └── collects measurements from sensors

Gateway
  └── processes and converts the received source data
```

---

## Position in the Source Device

```text
                      Sensor Layer
                 ============

        ┌───────────────┐
        │    ISensor    │
        │ initialize()  │
        │ read()        │
        │ name()        │
        └───────▲───────┘
                │
        implements
                │
   ┌────────────┼────────────┬──────────────┐
   │            │            │              │
   ▼            ▼            ▼              ▼
 DHT11       Shock        Light          Button
 Sensor      Sensor       Sensor          Sensor
```

`ISensor` is a contract, not a processing stage. `SensorConnector` accesses
the configured sensors through this interface and does not depend on their
concrete implementations.

What happens after `SensorConnector` — including the raw transport to the
gateway — is outside the responsibility of this layer.

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

`initialize()` prepares the sensor hardware.

`read()` performs one measurement and writes it into the caller's
`SensorReading`. It returns `true` when the measurement is usable and `false`
otherwise.

`name()` provides the sensor's identifier for diagnostics.

There is no separate validity flag in `SensorReading`; the return value of
`read()` determines whether the measurement is valid.

---

## SensorReading

`SensorReading` is the source-device structure used to pass one measurement
from a concrete sensor to `SensorConnector`.

It contains a sensor type, the corresponding sensor-specific reading and its
firmware timestamp. The full structure, including `SensorType` and the
sensor-specific reading types, is defined in `data_models.md`, which is the
single reference for the data model and is therefore not repeated here.

The timestamp is based on `millis()` and represents the time elapsed since the
ESP32 booted. It is not an absolute timestamp and is not used as the
Sparkplug timestamp by the gateway.

`SensorReading` is specific to the ESP32 firmware and is not part of the
gateway's internal `Metric` / `DeviceData` model.

---

## Current Implementations

Two ESP32 units currently run this firmware with different sensor
configurations. The active sensor set is selected at build time through
`SensorArray` in `main.cpp`.

All four implementations share the same `ISensor` interface, allowing
`SensorConnector` to handle them uniformly.

| Sensor         | Measures                 | Relevant behavior                                                                          |
| -------------- | ------------------------ | ------------------------------------------------------------------------------------------ |
| `DHT11Sensor`  | Temperature and humidity | `read()` returns `false` when the DHT library reports `NAN`.                               |
| `ShockSensor`  | Digital shock detection  | Can produce noisy transitions without a real shock; no software debouncing is implemented. |
| `LightSensor`  | Raw analog light level   | ADC value `0–4095`; not calibrated to lux.                                                 |
| `ButtonSensor` | Digital button state     | Uses the digital input state to report whether the button is pressed.                      |

None of the four current implementations reports a failed hardware
initialization; their `initialize()` methods currently return `true`.

---

## Design Principles

* **Single Responsibility.** A sensor only communicates with its own hardware and produces a reading.
* **Hardware Abstraction.** `ISensor` hides hardware-specific details from `SensorConnector`.
* **Common Interface.** Every sensor exposes the same three operations.
* **No Protocol Knowledge.** Nothing here knows about MQTT, Sparkplug, OPC UA or any other communication protocol.
* **No Gateway Knowledge.** A sensor produces a `SensorReading`; processing and transport are handled by other layers.

---

## Future Extensions

New sensors are added by implementing `ISensor` and extending the source-device
reading model documented in `data_models.md`.

Examples include a BME280 or a CO₂ sensor.

Adding a sensor remains a source-device change and does not require introducing
any gateway-specific concepts into the Sensor Layer.
