# Configuration

## Purpose

The `Configuration` class stores all application settings required by the
Industrial Edge Gateway.

It provides a single source of configuration for all software components
without introducing global state.

The class does not contain any application logic.

---

## Responsibilities

- Store application settings
- Validate configuration values
- Provide read-only access through getters

The class never

- communicates with sensors
- encodes Sparkplug messages
- publishes MQTT messages

---

## Configuration Parameters

### MQTT

- Broker Address
- Broker Port
- Client ID

### Sparkplug

- Group ID
- Edge Node ID
- Device ID

### Gateway

- Publish Interval

---

## Position in the Architecture

```text
                Configuration
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼

 SensorConnector  SparkplugEncoder  MQTTPublisher
```

Configuration is shared by multiple components through dependency injection.

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

    const char* deviceId() const;

    uint32_t publishInterval() const;
};
```

---

## Design Principles

- Single Responsibility Principle
- Dependency Injection
- Immutable access through getters
- No global variables
- No Singleton

---

## Future Extensions

Future versions may load the configuration from

- JSON files
- EEPROM
- SD Card

No other component has to change.