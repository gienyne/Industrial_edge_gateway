# Sensor Layer

## Purpose

The Sensor Layer provides a hardware abstraction for physical sensors
connected to a source device such as the ESP32.

Each sensor is responsible only for communicating with its own hardware,
acquiring measurements, validating the acquired data and producing a
source-specific `SensorReading`.

The Sensor Layer belongs to the source device and is independent from the
Industrial Edge Gateway.

The gateway does not access sensor hardware directly.

---

## Responsibilities

Each sensor is responsible for:

- initializing its hardware;
- acquiring measurements;
- validating the acquired data;
- returning a `SensorReading`;
- exposing its sensor identity through the common `ISensor` interface.

Sensors do **not**:

- create `Metric` objects;
- create `DeviceData`;
- encode Sparkplug messages;
- publish Sparkplug messages;
- implement gateway logic;
- communicate directly with the Industrial Edge Gateway.

---

## Position in the Architecture

The Sensor Layer is located entirely on the source device.

```text
                    ESP32 / SOURCE DEVICE
                    =====================

       DHT11Sensor      ShockSensor      LightSensor
            │                │                │
            │                │                │
            └────────────────┼────────────────┘
                             │
                             ▼
                     SensorConnector
                             │
                             ▼
                       Raw Transport

                    =====================
                         GATEWAY
```

`ISensor` is an interface implemented by the concrete sensor classes.
It is not a processing step in the data flow.

```text
DHT11Sensor ─────┐
ShockSensor ─────┼──────► SensorConnector
LightSensor ─────┘
       ▲
       │ implements
       │
     ISensor
```

The `SensorConnector` accesses the concrete sensors through the `ISensor`
interface and aggregates their readings.

The resulting source data is then transferred to the Industrial Edge Gateway
through a raw transport mechanism.

The concrete transport protocol is intentionally not defined at this layer.

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

The interface provides a common abstraction for all physical sensors.

A concrete sensor implements `ISensor` while keeping its hardware-specific
communication details internal to the implementation.

---

## Current Implementations

### DHT11Sensor

Measures:

- Temperature
- Humidity

Notes:

- The DHT11 communication protocol requires timing-sensitive bit transfers.
- The `read()` operation may block for a few milliseconds.

### ShockSensor

Measures:

- Shock detection

Acquisition method:

- Digital polling

Future extension:

- Interrupt-based acquisition

### LightSensor

Measures:

- Ambient light level

Current measurement:

- Raw ADC value

Future extension:

- Calibrated light intensity in lux

---

## Sensor Readings

Each sensor may define its own source-specific reading structure.

Examples:

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

These structures represent hardware-specific acquisition results.

They are not part of the Industrial Edge Gateway's common internal data
model.

The Gateway receives source data through its corresponding connector and
converts it into the common `Metric` and `DeviceData` representations.

---

## Design Principles

### Single Responsibility

Each sensor handles only its own hardware and measurement acquisition.

### Hardware Abstraction

The `ISensor` interface hides hardware-specific implementation details from
the `SensorConnector`.

### Common Sensor Interface

All sensors expose the same basic operations:

- initialization;
- measurement acquisition;
- sensor identification.

### No Protocol Knowledge

Sensors do not know about:

- MQTT;
- Sparkplug B;
- OPC UA;
- Modbus;
- HTTP;
- Gateway communication protocols.

### No Gateway Knowledge

Sensors are independent of the Industrial Edge Gateway.

They produce source-specific readings that are later handled by the
`SensorConnector` and transferred through the source-device transport layer.

---

## Future Extensions

New sensors can be added by implementing `ISensor`.

Examples:

- BME280
- CO₂ sensor
- additional digital sensors
- additional analog sensors

Adding a new sensor does not require changes to the gateway's common data
model or communication components.
