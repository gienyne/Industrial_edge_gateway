# Gateway Connectors

## Purpose

Gateway connectors integrate external source devices and industrial systems
into the Industrial Edge Gateway.

A connector speaks one source protocol and converts what it receives into the
gateway's common internal data model (`Metric` and `DeviceData`). Everything
downstream of the connectors is protocol-agnostic.

---

## Granularity: One Connector per Protocol

A connector represents a **protocol**, not a machine. One connector instance
can serve several devices:

| Connector        | Protocol      | Devices served                                                    |
|------------------|---------------|-------------------------------------------------------------------|
| `ESP32Connector` | MQTT + JSON   | every ESP32 publishing on the topic filter (`raw/+`); the device id comes from the topic |
| `OpcUaConnector` | OPC UA        | every entry of `sources`; each source (endpoint) is one device, identified by its configured `deviceId` |

Sparkplug standardization is done once, centrally, by the encoder. Adding a
second OPC UA machine is a configuration change (a new entry in `sources`),
not new code.

For OPC UA the model is "one endpoint = one machine": each machine of the
Smart Factory runs its own OPC UA server, there is no central multi-machine
server.

---

## Responsibilities

A connector

- communicates with its source system;
- acquires the source data;
- converts source-specific data into `Metric` objects;
- groups the metrics of each device into a `DeviceData`;
- supplies the device identity;
- recovers from source outages on its own (retry with cooldown).

A connector never

- encodes Sparkplug messages;
- publishes MQTT messages to the Sparkplug broker path;
- merges data from different devices;
- contains visualization or storage logic.

---

## Position in the Architecture

```text
Source Devices / Industrial Systems
                │
                │ source-specific communication
                ▼
        Gateway Connectors
                │
                │ common internal data model
                ▼
        std::vector<DeviceData>
                │
                ▼
       Gatewayapplication ──► SparkplugEncoder ──► Mqttpublisher ──► Broker
```

---

## IConnector Interface

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

Prepares the connector: loads credentials, creates clients, tries the first
connection. **A source that cannot be reached is not an initialization
failure.** The source stays registered and `collectData()` keeps retrying, so
the gateway can start before its sources exist.

### `collectData()`

Returns the current data of every device the connector can see, as zero or
more `DeviceData` objects. Each call is one acquisition cycle. Connectors
return the **current state**, never a delta: change detection (RBE) belongs to
`Gatewayapplication`.

By convention, a `DeviceData` with an empty `metrics` vector means "the device
was heard from but nothing usable was extracted" (a corrupted payload). It is a
liveness signal and nothing more. `ESP32Connector` uses it; `OpcUaConnector`
instead returns nothing for a source it could not read.

### `name()`

A human-readable identifier for logs and diagnostics.

---

## Timestamps

Connectors stamp `DeviceData.timestamp` and every `Metric.timestamp` with the
gateway's own clock (`nowMillis()` in `TimeUtils.h`, Unix epoch in
milliseconds). Neither source offers a trustworthy epoch timestamp: an ESP32
only knows its uptime, and the OPC UA `SourceTimestamp` is not read in the
current version.

---

## ESP32Connector

Receives the raw MQTT data published by ESP32 source devices.

```text
ESP32 ──► raw/<deviceId> (JSON) ──► ESP32Connector ──► DeviceData
```

- Subscribes to a wildcard topic filter (`raw/+`) with an asynchronous Paho
  client. The device id is extracted from the **topic**; the `deviceId` field
  inside the JSON is not used as the identity.
- Paho's callback thread only stores incoming payloads (protected by a mutex),
  keeping the most recent one per device. Parsing happens on the main thread
  inside `collectData()`, so the callback never touches application state. A
  call therefore returns at most one `DeviceData` per device.
- Maps the readings to metrics:

  | JSON `type` | Metrics                               |
  |-------------|----------------------------------------|
  | `DHT11`     | `temperature`, `humidity`              |
  | `SHOCK`     | `shockDetected`                        |
  | `LIGHT`     | `lightIntensity`                       |
  | `BUTTON`    | `buttonPressed`                        |

  plus `uptimeMs`: the firmware's `millis()`, exposed as a plain metric
  because it is uptime, not a date. A value that suddenly decreases reveals a
  reboot of the board.
- Corrupted JSON is caught; the device yields a `DeviceData` with no metrics
  (see `collectData()` above). Valid JSON with an unexpected structure is not an
  error: it yields a `DeviceData` containing only `uptimeMs`.
- If the broker connection is lost, `connection_lost` clears an atomic flag and
  `collectData()` reconnects and re-subscribes after a cooldown.

Configuration:

```cpp
struct ESP32ConnectorConfig
{
    std::string deviceId;       // MQTT client id of this connector, not a Sparkplug device id
    std::string brokerAddress;  // e.g. tcp://localhost:1883
    std::string topicFilter;    // e.g. raw/+
};
```

The `deviceId` field is passed to Paho as the client id of the connector's own
MQTT connection. It has to differ from the publisher's client id and from every
other client on the broker, because a broker drops the older of two clients
sharing an id. The name is misleading (`clientId` would be accurate).

The wire format of the raw transport is described in `data_models.md`.

---

## OpcUaConnector

Reads process variables from OPC UA servers with open62541pp.

```text
OPC UA server ──► OpcUaConnector ──► DeviceData (one per connected source)
```

- **Security.** Each source connects in `SignAndEncrypt` mode with a client
  certificate and username/password; with the CODESYS server the negotiated
  policy is `Basic256Sha256`. The certificate's SubjectAltName must contain the
  client's application URI (`urn:industrial-edge-gateway:opcua-probe`); the same
  certificate and URI are reused everywhere so the trust established on the
  server side stays valid. The certificate and key are read from disk on every
  connection attempt.
- **Polling, not subscriptions.** Each `collectData()` calls `runIterate()` and
  reads every configured node synchronously. This is a final design decision,
  not a stepping stone: OPC UA subscriptions would add a threading and
  notification model that five slowly-changing variables do not justify.
- **Lifetime.** Certificate, private key and client live together in the
  per-source runtime state. The client keeps references into the certificate
  buffers, so they must not be shorter-lived than the client.
- **Types.** A `REAL` variable arrives as an OPC UA `Float` and is delivered as a
  `Double` metric (`Double` also accepts a `Double` or `Int32` node). `Integer`
  reads an `Int32`. `String` is implemented but has not been tested against a
  real server.
- **Reconnection.** A lost session marks the source disconnected and retries
  after a cooldown (5 s). The attempt is synchronous: with the server down it
  blocks `collectData()` for the duration of the connection attempt (about 2 s
  against a closed local port), which delays every other source in the same
  cycle. A node that cannot be read is skipped and logged; the source still
  delivers its other metrics. If no metric can be read, the source yields no
  `DeviceData` at all.

Configuration:

```cpp
struct OpcUaMetricConfig  { std::string nodeId, metricName; MetricDataType dataType; std::string unit; };
struct OpcUaSourceConfig  { std::string endpoint, deviceId, username, password,
                            certificatePath, privateKeyPath;
                            std::vector<OpcUaMetricConfig> metrics; };
struct OpcUaConnectorConfig { std::vector<OpcUaSourceConfig> sources; };
```

The current source is AquaControl, a CODESYS irrigation simulation exposed as
the device `aquacontrol-opcua` with the metrics `tankLevel`, `pumpActive`,
`valveActive`, `waterConsumption` and `rainSimActive`. Its address space is
flat, so the whole application is one device.

Known limitations: the server certificate is not validated yet (no trust
store configured), and the OPC UA `SourceTimestamp` is not used.

---

## Failure Handling

| Situation                     | Connector        | Behaviour                                                                 |
|-------------------------------|------------------|---------------------------------------------------------------------------|
| MQTT connection lost          | `ESP32Connector` | flag cleared; reconnect and re-subscribe after the cooldown               |
| Corrupted JSON   | `ESP32Connector` | caught; `DeviceData` with no metrics                                      |
| OPC UA session lost           | `OpcUaConnector` | source marked disconnected; reconnect after the cooldown                  |
| OPC UA read error             | `OpcUaConnector` | metric skipped and logged; no `DeviceData` if every read fails                                       |
| Source unreachable at startup | both             | not fatal; retried from `collectData()`                                   |

In every case the rest of the gateway only observes **silence**. After
`deviceTimeout` the application publishes DDEATH for the device, and when the
source returns it is treated as a new device (rebirth). Connectors do not
publish anything about their own health.

---

## Device Identity

Each `DeviceData` carries the id of the device it describes. That id becomes
the Sparkplug Device ID in the topic (`spBv1.0/<group>/DDATA/<edgeNode>/<deviceId>`),
so it must be unique across all connectors.

| Connector        | Where the id comes from                              |
|------------------|------------------------------------------------------|
| `ESP32Connector` | the MQTT topic (`raw/<deviceId>`)                    |
| `OpcUaConnector` | `deviceId` of the `OpcUaSourceConfig`                |

The gateway has no global device id.

---

## Connector Configuration

Connector-specific settings are kept apart from the gateway-wide settings.
Each connector receives its own configuration structure at construction, and
`ConfigLoader` fills those structures from the JSON file (see
`configuration.md`).

---

## Multiple Connectors

`main.cpp` creates the connectors and hands them to `Gatewayapplication` as a
`std::vector<std::unique_ptr<IConnector>>`.

```text
ESP32Connector ──► DeviceData(esp32-dz), DeviceData(esp32-techz)
OpcUaConnector ──► DeviceData(aquacontrol-opcua)
                          │
                          ▼
             processed independently, per device
```

Connectors never merge their data, and never talk to each other.

---

## Separation from Source-Side Components

`IConnector` and the firmware's `SensorConnector` live on different sides of
the architectural boundary. `SensorConnector` aggregates local sensors on the
ESP32 and knows nothing about `Metric` or `DeviceData`; `ESP32Connector` is the
gateway-side counterpart that translates what the ESP32 publishes.

```text
SOURCE DEVICE                            GATEWAY
Sensors ─► SensorConnector ─► raw/<id> ═╪═► ESP32Connector ─► DeviceData
```

---

## Design Principles

- Single Responsibility
- Programming to the `IConnector` interface
- One connector per protocol, not per machine
- Common internal data model
- Failures handled locally, reported as silence
- Configuration injected, never hard-coded

---

## Future Extensions

- `ModbusConnector` for Modbus devices.
- `RESTConnector` for HTTP/REST interfaces.
- A second OPC UA machine of the Smart Factory: a new entry in `sources`.
- Reading the OPC UA `SourceTimestamp` (`readDataValue()`).
- Explicit OPC UA type inspection and server certificate validation.

The `IConnector` interface and `DeviceData` remain the stable integration
boundary.
