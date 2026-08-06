# Gateway Application

## Purpose

`GatewayApplication` is the composition root of the Industrial Edge Gateway.

It creates, initializes and connects all application components.

The class coordinates the complete application lifecycle without containing
business logic itself.

---

## Responsibilities

The `GatewayApplication`

- creates all application components
- injects dependencies
- initializes the gateway
- coordinates the main execution loop
- performs graceful shutdown

The application never

- communicates directly with hardware
- encodes Sparkplug messages
- publishes MQTT messages
- implements sensor-specific logic

---

## Position in the Architecture

```text
                GatewayApplication
                         │
     ┌───────────────────┼────────────────────┐
     ▼                   ▼                    ▼

Configuration      SensorConnector     SparkplugEncoder
        │                                   │
        └──────────────────┬────────────────┘
                           ▼

                   MQTTPublisher
```

`GatewayApplication` owns all major components and wires them together using
dependency injection.

---

## Public Interface

```cpp
class GatewayApplication
{
public:

    bool initialize();

    void run();

    void shutdown();
};
```

Typical usage

```cpp
int main()
{
    GatewayApplication app;

    if (!app.initialize())
    {
        return EXIT_FAILURE;
    }

    app.run();

    app.shutdown();

    return EXIT_SUCCESS;
}
```

---

## Internal Composition

```cpp
class GatewayApplication
{
private:

    Configuration configuration_;

    DHT11Sensor dht11Sensor_;

    ShockSensor shockSensor_;

    LightSensor lightSensor_;

    SensorArray sensors_;

    SensorConnector sensorConnector_;

    SparkplugEncoder sparkplugEncoder_;

    MQTTPublisher mqttPublisher_;
};
```

Member declaration order follows the dependency order shown above.

In C++, class members are always initialized in declaration order, regardless
of the constructor initializer list.

---

## Initialization Sequence

During startup the application performs the following steps.

```text
Create Configuration
        │
        ▼

Create Sensors
        │
        ▼

Create SensorArray
        │
        ▼

Create SensorConnector
        │
        ▼

Create SparkplugEncoder
        │
        ▼

Create MQTTPublisher
        │
        ▼

Initialize Sensors
        │
        ▼

Generate NDEATH (Last Will)
        │
        ▼

Connect to MQTT Broker
        │
        ▼

Publish NBIRTH
```

After successful initialization the gateway enters its normal operating mode.

---

## Main Execution Loop

The gateway continuously performs the following cycle.

```text
SensorConnector
        │
collectData()
        │
        ▼

DeviceData
        │
        ▼

SparkplugEncoder
        │
encode(DDATA)
        │
        ▼

SparkplugPayload
        │
        ▼

MQTTPublisher
        │
publish()
```

This cycle repeats until the application terminates.

---

## Shutdown Sequence

A graceful shutdown performs the following operations.

```text
Publish DDEATH
        │
        ▼

Disconnect MQTT
        │
        ▼

Terminate Application
```

If the application terminates unexpectedly, the MQTT Broker automatically
publishes the previously registered `NDEATH` message.

---

## Design Principles

- Composition Root
- Dependency Injection
- Separation of Responsibilities
- Explicit Application Lifecycle
- Single Responsibility Principle

---

## Future Extensions

Future versions may introduce additional components such as

- Logger
- Configuration Loader
- Database Storage
- Web Server
- OTA Update Manager
- Health Monitoring

These components can be integrated through `GatewayApplication` without
modifying the existing architecture.