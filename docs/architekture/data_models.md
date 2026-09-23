# Data Models

## Overview

The gateway is built around a common internal data model. Sources differ in
hardware, protocol and data format; each one is translated into `Metric` and
`DeviceData` at the gateway boundary, and everything after that point is
source-independent.

```text
           Source Device
                 │
                 ▼
           raw transport
                 │
 ═══════ transport boundary ═══════
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
Protobuf Payload (inside the encoder)
                 │
                 ▼
          SparkplugPayload
                 │
                 ▼
           Mqttpublisher
                 │
                 ▼
            MQTT Broker
```

For a sensor-based source such as the ESP32, a source-specific layer exists
before the boundary:

```text
Sensor ─> SensorReading ─> SourceData ─> JSON on raw/<deviceId> ═> ESP32Connector ─-> Metric
```

An OPC UA source has no such layer: the `OpcUaConnector` turns a node value
directly into a `Metric`.

Each model has one responsibility and one abstraction level.

---

## SensorReading (source side)

`SensorReading` is a source-specific measurement produced by one firmware
sensor. It is a tagged union: `type` says which member of `data` is valid.

```cpp
enum class SensorType { DHT11, SHOCK, LIGHT, BUTTON };

struct DHT11Reading {
float temperature;
float humidity;
unsigned long timestamp;
};

struct ShockReading {
bool detected;
unsigned long timestamp;
};

struct LightReading {
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

The `timestamp` fields are `millis()`: time since the board booted, not a date.
Readings belong to the firmware and are not part of the gateway model.

---

## Raw Transport (ESP32 → Gateway)

The boundary between the two sides is a JSON document published over MQTT.

```text
topic:    raw/<deviceId>
payload:  {
            "deviceId": "esp32-dz",
            "timestamp": 84213,
            "readings": [
              { "type": "DHT11",  "temperature": 23.6, "humidity": 53.0, "timestamp": 84210 },
              { "type": "SHOCK",  "detected": false, "timestamp": 84211 },
              { "type": "LIGHT",  "intensity": 592, "timestamp": 84212 },
              { "type": "BUTTON", "pressed": false, "timestamp": 84212 }
            ]
          }
```

Which readings appear depends on the sensors of that particular board.
Invalid readings are left out by the firmware's `SensorConnector`.

Device identity is determined by the MQTT topic. The `deviceId` field inside
the JSON payload is not used by the gateway for device identification.

---

## Metric

A `Metric` is one standardized measurement inside the gateway.

```cpp
enum class MetricDataType { Boolean, Integer, Double, String };

struct Metric
{
    std::string name;
    MetricDataType datatype;
    std::variant<bool, int, double, std::string> value;
    std::string unit;
    unsigned long long timestamp;     // Unix epoch, milliseconds
};
```

Metrics produced by the current connectors:

| Source                          | Metrics (type)                                                                               |
|---------------------------------|----------------------------------------------------------------------------------------------|
| ESP32 (DHT11)                   | `temperature` (Double, C), `humidity` (Double, %)                                            |
| ESP32 (shock, light, button)    | `shockDetected` (Boolean), `lightIntensity` (Integer), `buttonPressed` (Boolean)             |
| ESP32 (every payload)           | `uptimeMs` (Integer, ms)                                                                     |
| OPC UA `aquacontrol-opcua`      | `tankLevel` (Double, L), `pumpActive` (Boolean), `valveActive` (Boolean), `waterConsumption` (Double, L), `rainSimActive` (Boolean) |

The gateway does not care whether a value came from a DHT11, a CODESYS
variable or a Modbus register.

### Timestamps

Every `Metric.timestamp` and `DeviceData.timestamp` is set by the gateway at
collection time with `nowMillis()` (`TimeUtils.h`). Source clocks are not used
as Sparkplug timestamps: the ESP32 only knows its uptime, and the OPC UA
`SourceTimestamp` is not read in the current version. The ESP32's own
`millis()` is preserved as the separate `uptimeMs` metric. If that value drops
between two messages, the board has rebooted.

`DeviceData.timestamp` is the collection time of that whole snapshot, not a
per-metric time; each `Metric.timestamp` is its own. A `DeviceData` produced
by RBE filtering keeps the incoming snapshot's timestamp, even though the
metrics it carries may have been collected together with others that did not
change and were left out.

---

## DeviceData

`DeviceData` is the internal representation of one physical device: its
identity and the metrics collected in one cycle.

```cpp
struct DeviceData
{
    std::string deviceId;
    std::vector<Metric> metrics;
    unsigned long long timestamp;
};
```

- **Identity.** `deviceId` becomes the Sparkplug Device ID. It is unique across
  all connectors.
- **One device, one object.** Connectors never merge devices.
- **Two uses.** Connectors return the *full current state*. After RBE
  filtering, `Gatewayapplication` may use another `DeviceData` containing only
  the changed metrics.
- **Empty `metrics`.** The transport delivered something from the device but
  it could not be decoded (corrupted JSON). This is used as a liveness signal
  only: it refreshes the device's last-seen time and publishes nothing.
  `DeviceData` deliberately has no parsing status field: no consumer needs one
  yet. (Valid JSON with no recognized readings is not empty: it still carries
  `uptimeMs`.)

---

## SparkplugPayload

The communication model produced by the encoder and consumed by the publisher.

```cpp
using MqttTopic     = std::string;
using BinaryPayload = std::vector<uint8_t>;

struct SparkplugPayload
{
    MqttTopic     topic;
    BinaryPayload payload;    // serialized Sparkplug B Protobuf
    int           qos;
    bool          retain;
};
```

`seq` and `bdSeq` are not fields of this struct: they are written inside the
serialized bytes by the encoder. The publisher never needs them.

Between `DeviceData` and `SparkplugPayload` sits a third representation, the
Protobuf `Payload` generated from the Sparkplug B schema. It exists only inside
the encoder:

```text
DeviceData ──► Payload (Protobuf object) ──► bytes ──► SparkplugPayload
   internal        Sparkplug structure       serialized     transport envelope
```

---

## Data Ownership

| Model              | Ownership                         |
|--------------------|-----------------------------------|
| `SensorReading`    | Source-specific (firmware)        |
| Raw JSON           | Boundary contract                 |
| `Metric`           | Gateway internal                  |
| `DeviceData`       | Gateway internal                  |
| Protobuf `Payload` | Encoder internal                  |
| `SparkplugPayload` | Gateway communication             |

The gateway does not require a source device to know `Metric` or
`DeviceData`. A machine only has to be reachable by a connector.

---

## Design Principles

- **One abstraction level per model.** No structure mixes source, gateway and
  protocol concerns.
- **Hardware independence.** The core never sees `SensorReading`.
- **Protocol independence.** `Metric` and `DeviceData` know nothing about
  MQTT, Sparkplug, OPC UA or Modbus.
- **Centralized standardization.** Sparkplug B is produced once, by the
  encoder, so no source has to implement it.
- **Extensibility.** A new source needs a connector that produces `Metric` and
  `DeviceData`; the rest of the pipeline is unchanged.

---

## Known Limitations

- `uptimeMs` is stored as a 32-bit `Integer` and wraps after roughly 24.8 days
  without a reboot.
- `String` metrics are supported by the model and the encoder, and the OPC UA
  connector can read them, but that path has not been tested against a real
  server; the ESP32 firmware does not produce strings.
- Only corrupted JSON is signalled by empty `metrics`. A valid payload with an
  unexpected structure, or with no recognized readings, looks like a healthy
  device that reports only `uptimeMs`.
