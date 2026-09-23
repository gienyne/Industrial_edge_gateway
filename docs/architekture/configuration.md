# Configuration

## Purpose

All gateway and connector settings live in one JSON file, loaded at startup and
parsed into typed configuration structures. Each component receives exactly
the structure it needs at construction.

---

## Responsibilities

The `config` namespace (`ConfigLoader`)

* loads the JSON file;
* parses each section into the structure of the component that owns it;
* reports errors as exceptions.

It never

* communicates with sources or the broker;
* contains application logic;
* decides what happens after a configuration error (`main.cpp` does).

---

## Configuration Structures

Configuration is split by owner instead of being one large object:

| Structure                  | Owner                | Content                                                         |
| -------------------------- | -------------------- | --------------------------------------------------------------- |
| `GatewayApplicationConfig` | `Gatewayapplication` | encoder config, MQTT config, `discoveryWindow`, `deviceTimeout` |
| `SparkplugEncodeConfig`    | `SparkplugEncoder`   | `namespaceId`, `groupId`, `edgeNodeId`                          |
| `MQTTPublisherConfig`      | `Mqttpublisher`      | `brokerAddress`, `clientId`, `bdSeqFilePath`                    |
| `ESP32ConnectorConfig`     | `ESP32Connector`     | `deviceId` (MQTT client id), `brokerAddress`, `topicFilter`     |
| `OpcUaConnectorConfig`     | `OpcUaConnector`     | list of `OpcUaSourceConfig` (endpoint, credentials, metrics)    |

`GatewayApplicationConfig` embeds the encoder and publisher structures, so
`Gatewayapplication` can build both components itself.

The gateway has **no global device id**. Device identity belongs to the
connectors (see `gateway_connectors.md`).

---

## JSON Layout

The configuration is divided into two main sections:

* `gateway` contains gateway-wide settings such as MQTT, Sparkplug and
  lifecycle timeouts.
* `connectors` contains the configuration of each source connector.
  OPC UA sources are defined as entries in `connectors.opcua.sources`.

The concrete JSON structure and example values are provided by:

* `config/config.json.example`
* `config_docker/config.docker.json.example`

| JSON section       | Parsed by            | Result                     |
| ------------------ | -------------------- | -------------------------- |
| `gateway.*`        | `parseGatewayConfig` | `GatewayApplicationConfig` |
| `connectors.esp32` | `parseEsp32Config`   | `ESP32ConnectorConfig`     |
| `connectors.opcua` | `parseOpcUaConfig`   | `OpcUaConnectorConfig`     |

Adding a second OPC UA machine is one more object in `sources`; no code changes.

---

## Loading

```text
main.cpp
   │
   ├─ config::loadFile("config/config.json")   ─> nlohmann::json root
   ├─ config::parseEsp32Config(root)           ─> ESP32ConnectorConfig
   ├─ config::parseOpcUaConfig(root)           ─> OpcUaConnectorConfig
   └─ config::parseGatewayConfig(root)         ─> GatewayApplicationConfig
```

The path is relative to the working directory. A missing file, invalid JSON, a
missing required field or an unknown `dataType` raises an exception whose
message names the file or the JSON path (for example
`Missing required field "endpoint" in connectors.opcua.sources[]`); `main.cpp`
prints it and exits with code 1, so the gateway never starts with a half-valid
configuration. Each parse function handles one section, so a new connector adds
one function without touching the others.

Everything is required except three gateway settings, which have defaults:

| Optional field               | Default     |
| ---------------------------- | ----------- |
| `gateway.mqtt.bdSeqFilePath` | `bdseq.dat` |
| `gateway.discoveryWindowMs`  | `3000`      |
| `gateway.deviceTimeoutMs`    | `15000`     |

`dataType` accepts `Boolean`, `Integer`, `Double` or `String`. Both connector
sections (`esp32` and `opcua`) are currently mandatory, because `main.cpp`
creates both connectors unconditionally.

---

## Local and Docker Configuration

The two JSON files have the same structure. They differ in network addresses:
inside a container `127.0.0.1` and `localhost` designate the container itself,
so the broker and the OPC UA server running on the host are reached through
`host.docker.internal`.

| Setting                          | Local                      | Docker                                |
| -------------------------------- | -------------------------- | ------------------------------------- |
| `gateway.mqtt.brokerAddress`     | `tcp://localhost:1883`     | `tcp://host.docker.internal:1883`     |
| `connectors.esp32.brokerAddress` | `tcp://localhost:1883`     | `tcp://host.docker.internal:1883`     |
| `connectors.opcua…endpoint`      | `opc.tcp://127.0.0.1:4840` | `opc.tcp://host.docker.internal:4840` |

The Docker configuration and the certificates are mounted into the container
at run time, not copied into the image, so credentials never end up in an image
layer.

---

## Not Configurable Yet

* The polling period (500 ms) is fixed in `main.cpp`.
* The reconnection cooldowns (5 s) are compile-time constants.
* `deviceTimeout` is global, not per source.

---

## Source-Device Configuration

A source device has its own configuration, independent from the gateway. The
ESP32 firmware reads Wi-Fi credentials, broker address, `DEVICE_ID` and
`MQTT_CLIENT_ID` from `Config.h`.

```text
Source device                          Gateway
Config.h  (firmware)                   config/config.json
   │                                        │
   └── raw/<DEVICE_ID> over MQTT ──────────►┘  (only link between the two)
```

---

## Design Principles

* One configuration structure per owning component.
* Dependency injection: structures are passed in, never fetched globally.
* Fail fast on invalid configuration.
* Secrets outside the source code and outside Git.
* Gateway and source-device configuration are independent.

---

## Future Extensions

* Value validation (ranges, non-empty identifiers) after parsing.
* Overriding secrets with environment variables or Docker secrets.
* Per-source timeouts and polling intervals.
* Reloading the configuration without restarting.
