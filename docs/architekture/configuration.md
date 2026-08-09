# Configuration

## Purpose

The `Configuration` class stores all application settings required by the
Industrial Edge Gateway.

It provides a single source of gateway-level configuration without
introducing global state.

The class does not contain application logic.

---

## Responsibilities

The `Configuration` class

- stores gateway-level application settings;
- validates configuration values;
- provides read-only access through getters.

The class never

- communicates with sensors;
- acquires source data;
- encodes Sparkplug messages;
- publishes MQTT messages;
- contains application logic.

---

## Configuration Parameters

### MQTT

- Broker Address
- Broker Port
- Client ID

### Sparkplug

- Group ID
- Edge Node ID

### Gateway Runtime

- Publish Interval

These parameters apply to the gateway as a whole.

Source-specific configuration is not stored in the global
`Configuration` object.

---

## Position in the Architecture

```text
                 GatewayApplication
                         │
                         ▼
                  Configuration
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
      SparkplugEncoder        MQTTPublisher
```

Configuration is shared by gateway components through dependency injection.

Connector-specific configuration is handled separately by the corresponding
gateway connector.

---

## Connector Configuration

A gateway connector may require configuration specific to its source
device.

Connector-specific configuration is represented by a dedicated
configuration structure when required and is injected when the connector is
constructed.

For example:

```cpp
struct ESP32ConnectorConfig
{
    const char* deviceId;

    // Future transport-specific parameters
};
```

The connector receives this configuration during construction:

```cpp
ESP32Connector(const ESP32ConnectorConfig& config);
```

The global `Configuration` therefore does not contain a single global
`deviceId`.

The ownership and structure of connector-specific configuration are
documented in `gateway-connectors.md`.

The mechanism used to populate connector configuration remains open.

Possible future sources include

- hard-coded values;
- JSON configuration files;
- other external configuration sources.

Changing the configuration source does not require changes to the
`IConnector` interface or the common internal data model.

---

## Public Interface

```cpp
class Configuration
{
public:

    bool load();

    bool validate() const;

    const char* brokerAddress() const;

    uint16_t brokerPort() const;

    const char* clientId() const;

    const char* groupId() const;

    const char* edgeNodeId() const;

    uint32_t publishInterval() const;
};
```

The interface provides read-only access to the configuration values used by
the gateway components.

---

## Loading and Validation

Configuration loading and validation are separate responsibilities.

The `load()` method obtains the configuration values from the configured
source.

The `validate()` method checks whether the loaded values are valid for
gateway operation.

Conceptually:

```text
Configuration Source
        │
        ▼
      load()
        │
        ▼
   Configuration
        │
        ▼
     validate()
        │
        ▼
  GatewayApplication
```

The gateway should not start normal operation if the required gateway
configuration is invalid.

---

## Dependency Injection

The `Configuration` object is created by `GatewayApplication` and passed to
components that require gateway-level configuration.

```text
GatewayApplication
        │
        ▼
 Configuration
        │
 ┌──────┼────────────────┐
 ▼      ▼                ▼

Encoder  MQTTPublisher   other gateway components
```

This keeps configuration explicit and avoids hidden dependencies.

---

## Source-Device Configuration

Source devices may have configuration that is completely independent from
the gateway configuration.

For example, an ESP32 may have its own local configuration required for
sensor acquisition or source-side communication.

This configuration belongs to the source device and is not part of the
gateway `Configuration`.

The distinction is therefore:

```text
Source Device
┌─────────────────────────────┐
│ Local device configuration  │
│ Sensor configuration        │
│ Source-side parameters      │
└─────────────────────────────┘

             │
             │ Raw Transport
             ▼

Industrial Edge Gateway
┌─────────────────────────────┐
│ Gateway Configuration       │
│ Connector Configuration     │
└─────────────────────────────┘
```

Gateway configuration and source-device configuration are independent.

---

## Design Principles

- Single Responsibility Principle
- Dependency Injection
- Explicit configuration ownership
- Read-only access through getters
- No global variables
- No Singleton
- Separation of gateway and source-device configuration

---

## Future Extensions

Future versions may load gateway configuration from external sources such
as

- JSON files;
- environment variables;
- other configuration providers.

Connector-specific configuration may also be populated from external
configuration sources.

These changes should not require modifications to the common gateway data
model or the `IConnector` interface.
