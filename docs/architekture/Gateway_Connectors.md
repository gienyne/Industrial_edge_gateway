# Gateway Connectors

## Purpose

Gateway connectors connect external devices and industrial systems to the
Industrial Edge Gateway.

Each connector communicates with one source protocol and converts the received
data into the gateway's common internal data model: `Metric` and
`DeviceData`.

Everything behind the connectors works with this common model and does not
depend on the source protocol.

---

## Granularity: One Connector per Protocol

A connector represents a **protocol**, not a machine. One connector instance
can therefore handle several devices.

| Connector        | Protocol    | Devices served                                                                                            |
| ---------------- | ----------- | --------------------------------------------------------------------------------------------------------- |
| `ESP32Connector` | MQTT + JSON | Every ESP32 publishing through the configured topic filter (`raw/+`). The device ID comes from the topic. |
| `OpcUaConnector` | OPC UA      | Every entry in `sources`. Each source endpoint represents one device and uses its configured `deviceId`.  |

Sparkplug standardization happens only once, centrally in the encoder.

Adding another OPC UA machine therefore requires only a new entry in
`source`. No new connector code is needed.

For OPC UA, the current model is **one endpoint = one machine**. Each machine
in the Smart Factory runs its own OPC UA server; there is no central OPC UA
server for multiple machines.

---

## Responsibilities

A connector

* communicates with its source system;
* acquires the source data;
* converts source-specific data into `Metric` objects;
* groups the metrics of each device into a `DeviceData`;
* provides the device identity;
* handles source outages itself through retries with a cooldown.

A connector never

* creates Sparkplug messages;
* publishes MQTT messages to the Sparkplug broker path;
* combines data from different devices;
* contains visualization or storage logic.

---

## Position in the Architecture

```text
 ┌───────────────────────────────────────┐
 │ Source Devices / Industrial Systems   │
 └───────────────────┬───────────────────┘
                     │
                     │ source-specific protocol
                     ▼
            ┌──────────────────┐
            │ Gateway Connector│
            │                  │
            │ ESP32 / OPC UA   │
            └────────┬─────────┘
                     │
                     │ common data model
                     ▼
             ┌────────────────┐
             │   DeviceData   │
             │    + Metric    │
             └───────┬────────┘
                     │
                     ▼
            Gatewayapplication
                     │
                     ▼
              SparkplugEncoder
                     │
                     ▼
               Mqttpublisher
                     │
                     ▼
                MQTT Broker
```

The connector is the **protocol boundary**: source-specific data ends at the
connector, and `DeviceData` is what the rest of the gateway sees.

---

## `IConnector` Interface

```cpp
class IConnector
{
public:
    virtual ~IConnector() = default;

    virtual bool initialize() = 0;
    virtual std::vector<DeviceData> collectData() = 0;
    virtual const char* name() const = 0;
};
```

### `initialize()`

Prepares the connector by loading credentials, creating clients and trying
the first connection.

A source that cannot be reached is **not considered an initialization
failure**. The source remains registered, and `collectData()` keeps trying to
reconnect.

This allows the gateway to start even when its sources are not available yet.

### `collectData()`

Returns the current data of every device that the connector can currently
read, as zero or more `DeviceData` objects.

Each call represents one acquisition cycle.

Connectors always return the **current state**, not a delta. Change detection
and RBE are handled later by `Gatewayapplication`.

By convention, a `DeviceData` with an empty `metrics` vector means:

> The device was reached, but no usable metric could be extracted.

It is therefore a liveness signal and nothing more.

`ESP32Connector` uses this behavior for corrupted JSON. `OpcUaConnector`
instead returns no `DeviceData` when a source cannot provide any readable
metric.

### `name()`

Returns a human-readable name used for logging and diagnostics.

---

## Timestamps

Connectors assign `DeviceData.timestamp` and every `Metric.timestamp` using
the gateway's own clock:

```text
nowMillis()
    │
    ▼
TimeUtils.h
    │
    ▼
Unix epoch time in milliseconds
```

The current implementation does not use a trustworthy source-side epoch
timestamp:

* an ESP32 provides uptime rather than an absolute timestamp;
* the OPC UA `SourceTimestamp` is not read in the current version.

Using the gateway clock therefore gives all connectors the same timestamp
format.

---

# ESP32Connector

`ESP32Connector` receives raw MQTT messages published by ESP32 source devices.

```text
ESP32
  │
  │ MQTT / JSON
  ▼
raw/<deviceId>
  │
  ▼
ESP32Connector
  │
  │ Metric + DeviceData
  ▼
Gatewayapplication
```

### MQTT Reception

The connector subscribes to the configured wildcard topic filter, normally:

```text
raw/+
```

The device ID is extracted from the **topic**:

```text
raw/esp32-dz
    │
    └──► deviceId = "esp32-dz"
```

The `deviceId` field inside the JSON payload is not used as the device
identity.

Paho uses an asynchronous callback for incoming messages. The callback thread
only stores the latest payload for each device under a mutex.

Parsing happens later in `collectData()` on the main thread.

This keeps the callback independent from the gateway application state.

As a result, one `collectData()` call returns at most one `DeviceData` per
device.

### Data Mapping

The JSON payload is converted into the following metrics:

| JSON `type` | Metrics                   |
| ----------- | ------------------------- |
| `DHT11`     | `temperature`, `humidity` |
| `SHOCK`     | `shockDetected`           |
| `LIGHT`     | `lightIntensity`          |
| `BUTTON`    | `buttonPressed`           |

In addition, the firmware's `millis()` value is exposed as:

```text
uptimeMs
```

`uptimeMs` represents **uptime**, not a date.

If its value suddenly decreases, this indicates that the ESP32 has rebooted.

### Invalid and Unexpected Data

Corrupted JSON is caught during parsing.

The device then produces:

```text
DeviceData
    ├── deviceId
    ├── timestamp
    └── metrics = {}
```

This keeps the device's liveness information while publishing no unusable
data.

Valid JSON with an unexpected structure is handled differently. It is not
treated as a parsing error and produces a `DeviceData` containing only
`uptimeMs`.

### MQTT Reconnection

If the MQTT connection is lost:

```text
Paho connection_lost()
        │
        ▼
connected_ = false
        │
        ▼
collectData()
        │
        │ after cooldown
        ▼
reconnect + re-subscribe
```

The connector therefore handles its own MQTT reconnection.

### Configuration

```cpp
struct ESP32ConnectorConfig
{
    std::string deviceId;       // MQTT client id of this connector
    std::string brokerAddress;  // e.g. tcp://localhost:1883
    std::string topicFilter;    // e.g. raw/+
};
```

The `deviceId` field here is **not** the Sparkplug device ID.

It is the MQTT client ID used by the connector's own Paho connection.

It must be different from:

* the MQTT client ID of `Mqttpublisher`;
* the client ID of every other MQTT client on the broker.

Otherwise, the broker can disconnect the older client using the same client
ID.

The raw MQTT/JSON format is documented in `data_models.md`.

---

# OpcUaConnector

`OpcUaConnector` reads process variables from OPC UA servers using
`open62541pp`.

```text
┌────────────────────┐
│     OPC UA Server  │
│      (CODESYS)     │
└─────────┬──────────┘
          │
          │ OPC UA
          ▼
┌────────────────────┐
│   OpcUaConnector   │
└─────────┬──────────┘
          │
          │ DeviceData
          ▼
┌────────────────────┐
│   Gatewayapplication│
└────────────────────┘
```

One configured OPC UA source produces at most one `DeviceData` per acquisition
cycle.

---

## Security

Each source connects using:

* `SignAndEncrypt`;
* a client certificate;
* username/password authentication.

With the current CODESYS server, the negotiated security policy is
`Basic256Sha256`.

The certificate's `SubjectAltName` must contain the client's application URI:

```text
urn:industrial-edge-gateway:opcua-probe
```

The same certificate and application URI are reused for all connections so
that the trust established on the server side remains valid.

The certificate and private key are read from disk on every connection
attempt.

---

## Polling Instead of Subscriptions

The connector uses synchronous polling rather than OPC UA subscriptions.

Each `collectData()` call:

1. runs `runIterate()`;
2. reads every configured node;
3. converts the values into `Metric` objects;
4. returns the resulting `DeviceData`.

```text
collectData()
     │
     ├── runIterate()
     │
     ├── read metric 1
     ├── read metric 2
     ├── read metric 3
     └── ...
             │
             ▼
         DeviceData
```

This is the final design for the current application, not a temporary step
towards subscriptions.

The OPC UA data consists of only a few slowly changing variables. Introducing
subscriptions would add a separate notification and threading model without
providing a useful benefit for the current use case.

---

## Runtime Lifetime

The certificate, private key and OPC UA client are kept together in the
per-source runtime state.

The client keeps references to the certificate buffers, so those buffers must
remain alive for at least as long as the client.

---

## Data Types

The configured metric type determines how the OPC UA value is converted.

| Configuration type | OPC UA type | Gateway metric |
| ------------------ | ----------- | -------------- |
| `REAL`             | `Float`     | `Double`       |
| `REAL`             | `Double`    | `Double`       |
| `REAL`             | `Int32`     | `Double`       |
| `Integer`          | `Int32`     | `Integer`      |
| `String`           | String      | `String`       |

`String` is implemented but has not yet been tested against a real server.

---

## Reconnection and Read Errors

When an OPC UA session is lost:

```text
OPC UA session lost
        │
        ▼
source marked disconnected
        │
        ▼
wait for cooldown (5 s)
        │
        ▼
reconnect
```

The reconnect attempt is synchronous.

If the server is unavailable, the attempt can block `collectData()` for about
2 seconds against a closed local port. This delays the other sources handled
in the same polling cycle.

A node that cannot be read is skipped and the error is logged.

Other readable nodes of the same source continue to be processed.

If no metric can be read successfully, the source produces **no
`DeviceData`**.

---

## Configuration

```cpp
struct OpcUaMetricConfig
{
    std::string nodeId;
    std::string metricName;
    MetricDataType dataType;
    std::string unit;
};

struct OpcUaSourceConfig
{
    std::string endpoint;
    std::string deviceId;
    std::string username;
    std::string password;
    std::string certificatePath;
    std::string privateKeyPath;
    std::vector<OpcUaMetricConfig> metrics;
};

struct OpcUaConnectorConfig
{
    std::vector<OpcUaSourceConfig> sources;
};
```

The current source is **AquaControl**, a CODESYS irrigation simulation
exposed through OPC UA.

It is represented as:

```text
Device ID:
aquacontrol-opcua

Metrics:
├── tankLevel
├── pumpActive
├── valveActive
├── waterConsumption
└── rainSimActive
```

Its address space is flat, so the complete application is currently treated
as one device.

### Current OPC UA Limitations

* The server certificate is not validated yet because no trust store is
  configured.
* The OPC UA `SourceTimestamp` is not currently used.

---

# Failure Handling

| Situation                     | Connector behavior                                                                                |
| ----------------------------- | ------------------------------------------------------------------------------------------------- |
| MQTT connection lost          | `ESP32Connector` clears its connection flag, then reconnects and re-subscribes after the cooldown |
| Corrupted JSON                | `ESP32Connector` catches the error and returns `DeviceData` with no metrics                       |
| OPC UA session lost           | `OpcUaConnector` marks the source disconnected and retries after the cooldown                     |
| OPC UA read error             | The affected metric is skipped and the error is logged                                            |
| All OPC UA reads fail         | No `DeviceData` is returned for that source                                                       |
| Source unreachable at startup | Not fatal; the connector remains registered and retries from `collectData()`                      |

From the rest of the gateway's point of view, these failures result in
**silence**.

If a device remains silent longer than `deviceTimeout`, `Gatewayapplication`
publishes `DDEATH` and removes the device state.

When the source becomes available again, the device is treated as a new device
and therefore causes a rebirth.

Connectors do not publish their own health status.

---

# Device Identity

Every `DeviceData` contains the ID of the device it describes.

This ID becomes the Sparkplug Device ID in the MQTT topic:

```text
spBv1.0/<group>/DDATA/<edgeNode>/<deviceId>
```

Therefore, device IDs must be unique across **all connectors**.

| Connector        | Source of device ID                 |
| ---------------- | ----------------------------------- |
| `ESP32Connector` | MQTT topic: `raw/<deviceId>`        |
| `OpcUaConnector` | `deviceId` from `OpcUaSourceConfig` |

There is no global device-ID generator in the gateway.

Each connector is responsible for providing a valid and unique device ID.

---

# Connector Configuration

Connector-specific settings are kept separate from gateway-wide settings.

Each connector receives its own configuration structure during construction.

`ConfigLoader` reads the JSON configuration and creates the corresponding
configuration objects.

The complete configuration format is documented in `configuration.md`.

---

# Multiple Connectors

`main.cpp` creates all connectors and passes them to `Gatewayapplication` as:

```cpp
std::vector<std::unique_ptr<IConnector>>
```

For example:

```text
                     Gatewayapplication
                              │
             ┌────────────────┴────────────────┐
             │                                 │
             ▼                                 ▼
      ESP32Connector                     OpcUaConnector
             │                                 │
      ┌──────┴──────┐                          │
      ▼             ▼                          ▼
  esp32-dz      esp32-techz             aquacontrol-opcua
      │             │                          │
      └─────────────┴──────────────┬───────────┘
                                   ▼
                             DeviceData
```

Each device is processed independently.

Connectors never merge their data and never communicate with each other.

---

# Separation from Source-Side Components

`IConnector` and the firmware-side `SensorConnector` belong to different
parts of the system.

The `SensorConnector` runs **inside the ESP32**. It collects local sensor
values and knows nothing about `Metric` or `DeviceData`.

The gateway-side `ESP32Connector` receives the data published by the ESP32
and translates it into the gateway's common model.

```text
                 ESP32                              GATEWAY

 ┌─────────────────────────┐
 │ Sensors                 │
 │   │                     │
 │   ▼                     │
 │ SensorConnector         │
 │   │                     │
 └───┼─────────────────────┘
     │
     │ raw/<deviceId>
     │ MQTT / JSON
     ▼
 ═════════════════════════════════════════════════════════════
                              │
                              ▼
                    ESP32Connector
                              │
                              │ Metric / DeviceData
                              ▼
                     Gatewayapplication
```

This boundary keeps source-side sensor handling separate from gateway-side
protocol conversion.

---

# Design Principles

* **Single Responsibility:** each connector handles one source protocol.
* **`IConnector` interface:** the gateway works with connectors through a
  common interface.
* **One connector per protocol:** a connector can serve multiple machines or
  devices of the same protocol.
* **Common internal model:** all source-specific data becomes `Metric` and
  `DeviceData`.
* **Local failure handling:** connectors handle their own connection and retry
  logic; the rest of the gateway sees unavailable sources as silence.
* **Configuration instead of hard-coding:** source-specific settings are
  provided through configuration structures.

---

# Future Extensions

* `ModbusConnector` for Modbus devices.
* `RESTConnector` for HTTP/REST interfaces.
* Additional OPC UA machines by adding another entry to `sources`.
* Reading the OPC UA `SourceTimestamp` through `readDataValue()`.
* Explicit OPC UA type inspection.
* OPC UA server certificate validation.

The `IConnector` interface and `DeviceData` remain the stable integration
boundary for additional source protocols.
