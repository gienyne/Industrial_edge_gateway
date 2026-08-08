# Sparkplug Encoder

## Purpose

The `SparkplugEncoder` converts the gateway's internal data model into
Sparkplug B compliant messages.

It is the only component that knows the Sparkplug B protocol.

The encoder is completely independent from sensors, source devices,
MQTT transport and hardware.

---

## Responsibilities

The `SparkplugEncoder`

- builds Sparkplug B topics
- encodes Protobuf payloads
- maintains Sparkplug sequence numbers
- produces binary payloads ready for transmission

The encoder never

- communicates with sensors
- publishes MQTT messages
- stores measurements
- communicates with databases

---

## Position in the Architecture

```text
                    Industrial Edge Gateway
                             │
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

The encoder belongs exclusively to the central Industrial Edge Gateway.

Source devices such as the ESP32 do not perform Sparkplug encoding.

---

## Public Interface

```cpp
class ISparkplugEncoder
{
public:

    virtual ~ISparkplugEncoder() = default;

    virtual SparkplugPayload encode(const DeviceData& deviceData,MessageType messageType) = 0;
};
```

The interface separates Sparkplug-specific encoding from the gateway
application and the MQTT transport.

It also allows the encoding strategy to be replaced in the future without
modifying the surrounding gateway pipeline.

---

## Supported Message Types

```cpp
enum class MessageType
{
    NBIRTH,
    DBIRTH,
    DDATA,
    NDEATH,
    DDEATH
};
```

Command messages such as `NCMD` and `DCMD` are outside the responsibility
of the encoder.

They will be handled by a dedicated Sparkplug command/decoder component
if command support is required in the future.

---

## Internal Responsibilities

Internally, the encoder performs several independent steps.

```text
DeviceData
    │
    ▼
Build Topic
    │
    ▼
Create Protobuf Payload
    │
    ▼
Append Metrics
    │
    ▼
Update Sequence Numbers
    │
    ▼
SparkplugPayload
```

The public interface exposes a single `encode()` function.

The implementation may delegate individual steps to private helper
methods.

---

## Sequence Management

The encoder maintains the Sparkplug sequence counters.

```cpp
uint8_t bdSeq_;
uint8_t seq_;
```

### bdSeq

`bdSeq` represents the Birth/Death sequence of the Edge Node.

- changes when a new Edge Node session starts
- remains constant during normal operation
- is used to associate Birth and Death messages with the same session

### seq

`seq` represents the message sequence number.

- increments for each Sparkplug message
- wraps from 255 back to 0
- is reset when a new NBIRTH message starts a new session

---

## Protobuf Encoding

Sparkplug B uses Protocol Buffers for its payload representation.

The encoder does not implement the binary encoding manually.

Instead, the Sparkplug B Protobuf schema is used together with an
appropriate Protobuf implementation to serialize the payload.

Conceptually:

```text
DeviceData
    │
    ▼
SparkplugEncoder
    │
    ├── Build Sparkplug Topic
    │
    ├── Populate Protobuf Message
    │
    └── Serialize Protobuf Message
            │
            ▼
       Binary Payload
```

The encoder is therefore responsible for mapping the gateway's
`DeviceData` model to the Sparkplug B message structure, while the
Protobuf library performs the binary serialization.

---

## Produced Output

The encoder produces a complete Sparkplug message ready for transport.

```cpp
struct SparkplugPayload
{
    MqttTopic topic;
    BinaryPayload payload;
};
```

The `MQTTPublisher` does not interpret Sparkplug messages.

It only publishes the topic and binary payload produced by the encoder.

---

## MQTT Last Will

The encoder is responsible for creating the Sparkplug-specific information
required for an NDEATH message.

The MQTT Publisher remains responsible for configuring the MQTT Last Will
during connection establishment.

This separation keeps Sparkplug-specific message knowledge inside the
encoder while keeping the MQTT Publisher focused on transport operations.

---

## Design Principles

### Single Responsibility

The encoder is responsible only for translating the internal gateway data
model into Sparkplug B messages.

### Protocol Encapsulation

All Sparkplug-specific knowledge is isolated inside the encoder.

### Transport Independence

The encoder does not establish MQTT connections or publish messages.

### Hardware Independence

The encoder has no knowledge of sensors, PLCs, robots or other physical
devices.

### Internal Model Independence

The encoder consumes the common `DeviceData` representation and therefore
does not depend on any specific source protocol.

---

## Future Extensions

Future versions may support additional Sparkplug B features such as

- Sparkplug Templates
- Dataset Metrics
- Property Sets
- Metric Aliases
- Compression
- additional Sparkplug message types

Command messages such as `NCMD` and `DCMD` may be implemented later by a
dedicated Sparkplug command/decoder component.

Alternative encoding or standardization strategies may also be introduced
in the future without requiring changes to the gateway's data acquisition
and internal data model layers.

---

## Summary

The `SparkplugEncoder` forms the standardization boundary of the
Industrial Edge Gateway.

It receives the protocol-independent `DeviceData` model and converts it
into Sparkplug B topics and binary payloads.

```text
Source Devices
      │
      ▼
   Connectors
      │
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

Sparkplug B is therefore a gateway-level output protocol rather than a
responsibility of the individual source devices.
