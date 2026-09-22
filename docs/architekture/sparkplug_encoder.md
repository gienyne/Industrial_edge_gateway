# Sparkplug Encoder

## Purpose

`SparkplugEncoder` converts the gateway's internal data model into Sparkplug B
messages.

It is the only component that knows the Sparkplug B message format, including
topics, Protobuf payloads, metric declarations and sequence numbers.

The encoder is independent of sensors, source protocols and MQTT transport.

---

## Responsibilities

The encoder

* builds Sparkplug topics;
* creates and serializes Sparkplug B Protobuf payloads;
* maintains the message sequence number (`seq`);
* stores the MQTT session's birth/death sequence number (`bdSeq`);
* defines the QoS and retain flag for each message type;
* builds the MQTT Last Will payload (`NDEATH`).

The encoder never

* reads sensors or communicates with source systems;
* connects to or publishes messages through the MQTT broker;
* decides **when** a message is sent — this is handled by
  `Gatewayapplication`;
* decodes incoming commands.

---

## Position in the Architecture

```text
                    Gateway
                       │
                       │ DeviceData
                       ▼
              ┌──────────────────┐
              │ SparkplugEncoder │
              │                  │
              │ topics           │
              │ Protobuf         │
              │ seq / bdSeq      │
              │ QoS / retain     │
              └────────┬─────────┘
                       │
                       │ SparkplugPayload
                       ▼
                Mqttpublisher
                       │
                       │ MQTT
                       ▼
                  MQTT Broker
```

The encoder belongs entirely to the gateway.

Source devices never create Sparkplug messages. They only provide source data
that is converted into `DeviceData` by the connectors.

---

## Public Interface

The interface separates **Node-level** messages from **Device-level**
messages.

A Node message describes the Edge Node itself, not a device. Using one generic
function such as `encode(deviceData, messageType)` would therefore require a
dummy `DeviceData` for `NBIRTH` or `NDEATH`.

The interface keeps this distinction explicit:

```cpp
class IsparkplugEncoder
{
public:
    virtual ~IsparkplugEncoder() = default;

    // Node level
    virtual SparkplugPayload encodeNodeBirth() = 0;
    virtual SparkplugPayload encodeNodeDeath() = 0;

    // Device level
    virtual SparkplugPayload encodeDeviceBirth(const DeviceData& deviceData) = 0;
    virtual SparkplugPayload encodeDeviceData(const DeviceData& deviceData) = 0;
    virtual SparkplugPayload encodeDeviceDeath(const std::string& deviceId) = 0;

    // Session helpers
    virtual std::string nodeCommandTopic() const = 0;
    virtual SparkplugPayload buildWillPayload() = 0;
};
```

`SparkplugEncoder` implements this interface and additionally provides
`setBdSeq(std::uint64_t)`.

`setBdSeq()` is called when a new MQTT session starts. It stores the session's
`bdSeq` and resets the message sequence counter `seq`.

### Configuration

```cpp
struct SparkplugEncodeConfig
{
    std::string namespaceId;
    std::string groupId;
    std::string edgeNodeId;
};
```

---

## Topics

Sparkplug topics follow this structure:

```text
spBv1.0/<groupId>/<verb>/<edgeNodeId>[/<deviceId>]
```

`nodeCommandTopic()` returns the `NCMD` topic used by the publisher to receive
Node commands.

---

## Messages

| Message  | Level  |      `seq` | Content                                       |
| -------- | ------ | ---------: | --------------------------------------------- |
| `NBIRTH` | Node   |        `0` | `bdSeq` and `Node Control/Rebirth = false`    |
| `NDEATH` | Node   |       none | `bdSeq`                                       |
| `DBIRTH` | Device | next value | All metrics of the device, including datatype |
| `DDATA`  | Device | next value | Changed metrics only, without datatype        |
| `DDEATH` | Device | next value | No metrics                                    |

### QoS and Retain

QoS and retain are defined by the encoder rather than by the publisher.

The current behavior is:

| Message              | QoS | Retain |
| -------------------- | --: | ------ |
| `NBIRTH`             |   0 | false  |
| `DBIRTH`             |   0 | false  |
| `DDATA`              |   0 | false  |
| `DDEATH`             |   0 | false  |
| `NDEATH` / Last Will |   1 | false  |

The publisher transports these values unchanged.

Birth, data and `DDEATH` payloads contain the encoding time as the payload
timestamp.

Each metric keeps its own collection timestamp from `Metric.timestamp`.

---

# Sequence Numbers

Sparkplug uses two different sequence numbers with different purposes:

```text
                    Sparkplug session
                          │
             ┌────────────┴────────────┐
             │                         │
             ▼                         ▼
          bdSeq                       seq
       MQTT session ID          message sequence
             │                         │
             │                         ├── NBIRTH = 0
             │                         ├── DBIRTH = 1
             │                         ├── DBIRTH = 2
             │                         ├── DDATA  = 3
             │                         └── ...
             │
             └── same value in
                 NBIRTH + NDEATH
```

## `seq`

`seq` is one counter shared by Node and Device messages.

It is a `uint8_t`, so it naturally wraps from `255` back to `0`.

Every `NBIRTH` resets the counter to `0`, including an `NBIRTH` published for a
logical rebirth inside the same MQTT session.

The following messages consume the next values:

```text
NBIRTH  → 0
DBIRTH  → 1
DBIRTH  → 2
DDATA   → 3
DDATA   → 4
DDEATH  → 5
...
```

`NDEATH` and `NCMD` do not carry `seq`.

A Host Application can use the sequence to detect missing Sparkplug messages.

The Sparkplug 3.0.0 specification is not completely consistent on the initial
`NBIRTH` value:

* the operational chapter allows any starting value from `0` to `255`;
* the conformance chapter requires `0`.

Using `0` satisfies both interpretations.

---

## `bdSeq`

`bdSeq` identifies the MQTT session.

It is set once at the beginning of a session through `setBdSeq()` and remains
unchanged for that entire session.

The same `bdSeq` is used in:

```text
             MQTT session
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
     NBIRTH               NDEATH
        │                   │
        └────── same ───────┘
              bdSeq
```

This allows a Host Application to associate an `NDEATH` with the `NBIRTH`
belonging to the same MQTT session.

### Logical Rebirth

A logical rebirth, for example after:

* a new device;
* a new metric;
* an `NCMD` Rebirth request;

publishes a new `NBIRTH` but keeps the same `bdSeq`.

The MQTT connection did not change, so the Last Will already registered with
the broker still belongs to the same session.

Only a **new MQTT connection** produces a new `bdSeq`.

---

## `bdSeq` Persistence

`SparkplugEncoder` stores the current `bdSeq`, but does not choose or persist
it.

That responsibility belongs to `BdSeqManager`.

The value is stored in a small file, `bdseq.dat` by default:

```text
nextSessionBdSeq()
       │
       │ read file
       │ increment
       │ 255 → 0
       │ missing / unreadable → 0
       ▼
 encoder.setBdSeq()
       │
       │ use value for session
       ▼
 MQTT CONNECT succeeds
       │
       ▼
commitSessionBdSeq()
       │
       ▼
 bdseq.dat updated
```

The value is written back **only after the MQTT `CONNECT` succeeds**.

Therefore, a failed connection attempt does not consume a `bdSeq` value.

The file is generated by the gateway and is not version-controlled.

---

# Protobuf Encoding

The Sparkplug B schema is defined in:

```text
proto/sparkplug_b.proto
```

It is compiled during the build with `protobuf_generate`.

The generated Protobuf class is:

```cpp
org::eclipse::tahu::protobuf::Payload
```

The encoder maps the gateway's internal data model onto this Protobuf structure.

```text
Internal Metric
      │
      │ appendMetric()
      ▼
Payload::Metric
(Protobuf)
      │
      ▼
Payload
      │
      │ ByteSizeLong()
      │ resize()
      │ SerializeToArray()
      ▼
SparkplugPayload
```

There are therefore two different types called `Metric`:

* the gateway's internal `Metric`;
* `Payload::Metric` from the Sparkplug Protobuf definition.

`appendMetric()` performs the conversion between them.

The `includeDatatype` parameter controls whether the Protobuf metric contains
its datatype.

### Datatype Mapping

| `MetricDataType` | Sparkplug datatype |
| ---------------- | ------------------ |
| `Boolean`        | Boolean            |
| `Integer`        | Int32              |
| `Double`         | Double             |
| `String`         | String             |

Serialization is not transmission.

`SerializeToArray()` only creates the serialized bytes. Sending those bytes
over MQTT is the responsibility of `Mqttpublisher`.

---

# Produced Output

The encoder returns a transport-ready `SparkplugPayload`:

```cpp
struct SparkplugPayload
{
    MqttTopic     topic;     // std::string
    BinaryPayload payload;   // std::vector<uint8_t>, serialized Protobuf
    int           qos;
    bool          retain;
};
```

The publisher does not interpret the Sparkplug content.

It simply transports:

```text
SparkplugPayload
       │
       ├── topic
       ├── payload
       ├── qos
       └── retain
              │
              ▼
         Mqttpublisher
              │
              ▼
          MQTT Broker
```

---

# MQTT Last Will

`buildWillPayload()` creates the `NDEATH` message used as the MQTT Last Will.

The responsibilities are deliberately separated:

```text
SparkplugEncoder
    │
    │ creates NDEATH payload
    ▼
SparkplugPayload
    │
    │ publisher registers it
    ▼
Mqttpublisher
    │
    ▼
MQTT CONNECT
```

The encoder knows **what the Last Will contains**.

The publisher knows **how to register it with MQTT**.

---

# Not Supported

The following features are intentionally outside the current scope:

* Metric aliases;
* Templates;
* DataSets;
* PropertySets;
* `NDATA` for dynamic Node-level metrics;
* `DCMD`;
* decoding incoming commands.

The Node Rebirth command is detected by `Mqttpublisher`, which owns the MQTT
subscription.

The encoder only provides the corresponding `nodeCommandTopic()`.

---

# Design Principles

* **Single Responsibility:** the encoder handles Sparkplug message creation
  and nothing else.
* **Protocol encapsulation:** all Sparkplug-specific knowledge is kept here.
* **Transport independence:** the encoder creates messages but does not send
  them.
* **Separate Node and Device operations:** the interface reflects the actual
  Sparkplug message hierarchy.
* **Protocol-free internal model:** `Metric` and `DeviceData` do not contain
  Sparkplug-specific concepts.

---

# Future Extensions

* Metric aliases to reduce payload size.
* Templates, DataSets and PropertySets.
* `NDATA` for Node-level metrics.
* A dedicated command decoder if additional `NCMD` or `DCMD` commands are
  required.
