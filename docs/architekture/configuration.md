# Configuration

## Purpose

The `Configuration` class stores the runtime settings required by the
Industrial Edge Gateway.

It provides a single source of configuration for the gateway components
without introducing global state.

The configuration belongs to the Industrial Edge Gateway and is independent
from the configuration of individual source devices.

The class does not contain application logic.

---

## Responsibilities

The `Configuration` class

- stores gateway application settings;
- validates configuration values;
- provides read-only access through getters.

The class never

- communicates with source devices;
- reads sensor data;
- creates metrics;
- encodes Sparkplug messages;
- publishes MQTT messages.

---

## Configuration Ownership

Configuration is separated according to the architectural boundary.

```text
+-------------------------+
| Source Device           |
|                         |
| Local Configuration     |
|                         |
| - sensor settings       |
| - acquisition settings  |
| - source-specific data  |
+------------┬------------+
             │
             │ Raw Transport
             ▼
+-------------------------+
| Industrial Edge Gateway |
|                         |
| Configuration           |
|                         |
| - MQTT settings         |
| - Sparkplug settings    |
| - gateway runtime       |
+-------------------------+
```

Source devices may therefore have their own local configuration without
becoming dependent on the gateway configuration model.

---

## Configuration Parameters

### MQTT

The gateway configuration contains the parameters required to communicate
with the MQTT broker.

- Broker Address
- Broker Port
- Client ID

### Sparkplug

The gateway configuration contains the parameters required for Sparkplug B
publication.

- Group ID
- Edge Node ID

### Gateway Runtime

The gateway configuration contains runtime parameters controlling the
gateway application.

- Publish Interval

---

## Position in the Architecture

```text
                         Configuration
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
       IConnector      ISparkplugEncoder  IMqttPublisher
              │                │                │
              │                │                │
              └────────────────┼────────────────┘
                               │
                       GatewayApplication
```

Configuration is provided to the gateway components through dependency
injection.

The configuration does not determine how individual source devices are
accessed.

That responsibility belongs to the corresponding connector.

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

The interface provides read-only access to configuration values.

Configuration values are not modified directly by other components.

---

## Loading and Validation

Configuration loading and validation are separate operations.

```text
load()
  │
  ▼
Configuration values
  │
  ▼
validate()
  │
  ├── valid ──────► Gateway startup
  │
  └── invalid ────► Startup failure
```

`load()` obtains the configuration values from the configured source.

`validate()` checks whether the loaded values are valid for the gateway.

The `GatewayApplication` is responsible for invoking these operations during
startup.

---

## Dependency Injection

`Configuration` is created by the `GatewayApplication` and passed to the
components that require gateway configuration.

```text
GatewayApplication
        │
        ▼
 Configuration
        │
        ├──────────────► SparkplugEncoder
        │
        └──────────────► MQTTPublisher
```

The configuration is therefore explicit and traceable through the
application dependencies.

No component accesses configuration through global variables or a
Singleton.

---

## Separation from Source-Device Configuration

The gateway configuration must not contain parameters that belong
exclusively to a source device.

For example, sensor-specific acquisition parameters remain on the source
device:

```text
ESP32
 ├── Sensor configuration
 ├── Acquisition settings
 └── Raw data transmission settings
```

while the gateway manages:

```text
Industrial Edge Gateway
 ├── MQTT connection
 ├── Sparkplug identity
 └── Gateway runtime
```

This separation prevents the gateway configuration model from becoming
coupled to a specific source-device implementation.

---

## Device Identity

Each source device must have its own identity so that the gateway can
distinguish between multiple devices.

The `deviceId` is therefore not part of the global `Configuration` model.

The exact ownership and provisioning mechanism for source-device identities
remains an open architectural decision.

Possible approaches include

- providing the device ID when constructing a connector;
- defining a connector-specific configuration;
- loading device identities from a future gateway configuration source.

This decision will be made when the concrete connector architecture is
designed.

---

## Design Principles

- Single Responsibility Principle
- Dependency Injection
- Explicit configuration ownership
- Read-only access
- No global variables
- No Singleton
- Separation between gateway and source-device configuration

---

## Future Extensions

Future versions may load gateway configuration from different sources,
such as:

- JSON files
- Environment variables
- Configuration files
- EEPROM
- SD Card

The configuration interface can be extended without requiring changes to
the gateway components that consume it.
