# Sensor Layer

## Purpose

The Sensor Layer provides a hardware abstraction for all physical sensors.

Each sensor is responsible only for communicating with its own hardware and
producing a common `SensorReading` object.

The remaining gateway components never access hardware directly.

---

## Responsibilities

Each sensor

- initializes its hardware
- acquires measurements
- validates the acquired data
- returns a `SensorReading`

Sensors never

- create metrics
- build `DeviceData`
- encode Sparkplug messages
- publish MQTT messages

---

## Position in the Architecture

```text
                   Sensor Layer

      +-------------+-------------+
      |             |             |
      ▼             ▼             ▼

 DHT11Sensor   ShockSensor   LightSensor
      │             │             │
      └─────────────┼─────────────┘
                    ▼

                ISensor
                    │
                    ▼

            SensorConnector
```

---

## Public Interface

```cpp
class ISensor
{
public:

    virtual bool initialize() = 0;

    virtual SensorReading read() = 0;

    virtual const char* name() const = 0;

    virtual ~ISensor() = default;
};
```

---

## Current Implementations

### DHT11Sensor

Measures

- Temperature
- Humidity

Notes

- The DHT11 communication protocol requires timing-sensitive bit transfers.
- The `read()` operation may block for a few milliseconds.

---

### ShockSensor

Measures

- Shock detection

Acquisition Method

- Digital polling

Future Extension

- Interrupt-based acquisition

---

### LightSensor

Measures

- Ambient light level

Measurement

- Raw ADC value

Future Extension

- Calibrated light intensity (lux)

---

## Design Principles

- Single Responsibility Principle
- Hardware abstraction
- Common sensor interface
- No protocol knowledge
- No gateway knowledge

---

## Future Extensions

New sensors can be added by implementing `ISensor`.

Examples

- BME280
- CO₂ Sensor

No modification of existing gateway components is required.