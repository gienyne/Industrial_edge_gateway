# Gateway Application

## Purpose

`GatewayApplication` is the composition root of the Industrial Edge Gateway.

It creates, initializes and connects all application components.

The class coordinates the complete application lifecycle without containing
business logic itself.

---

## Responsibilities

The `GatewayApplication`

- creates all application components;
- injects dependencies;
- initializes the gateway;
- coordinates the main execution loop;
- performs graceful shutdown.

The application never

- communicates directly with hardware;
- implements sensor-specific logic;
- encodes Sparkplug messages;
- publishes MQTT messages;
- processes protocol-specific data.

---

## Position in the Architecture

```text
                         GatewayApplication
                                  │
             ┌────────────────────┼────────────────────┐
             │                    │                    │
             ▼                    ▼                    ▼
       Configuration          IConnector       ISparkplugEncoder
                                  │                    │
                    ┌─────────────┼─────────────┐      │
                    │             │             │      │
                    ▼             ▼             ▼      │
             ESP32Connector   OPCUAConnector  ModbusConnector
                    │             │             │      │
                    ▼             ▼             ▼      │
               DeviceData    DeviceData    DeviceData  │
                    │             │             │      │
                    └─────────────┼─────────────┘      │
                                  │                    │
                                  └──────────┬─────────┘
                                             ▼
                                      SparkplugPayload
                                             │
                                             ▼
                                      IMqttPublisher
                                             │
                                             ▼
                                        MQTT Broker
```

`GatewayApplication` does not know how individual machines are accessed.

It only coordinates components through their defined interfaces.

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

Typical usage:

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

    std::vector<std::unique_ptr<IConnector>> connectors_;

    std::unique_ptr<ISparkplugEncoder> sparkplugEncoder_;

    std::unique_ptr<IMqttPublisher> mqttPublisher_;
};
```

The `GatewayApplication` depends on interfaces where multiple
implementations or test substitutions are intended.

- `IConnector` allows different machine protocols to be integrated.
- `ISparkplugEncoder` allows alternative encoding strategies to be
  substituted in the future.
- `IMqttPublisher` allows the concrete MQTT implementation to be replaced,
  for example by a mock during testing or by another MQTT client library.

The concrete implementations are created by the composition root and
injected into the application.

---

## Member Initialization Order

Member declaration order follows the dependency order shown above.

In C++, class members are always initialized in declaration order,
regardless of the order used in the constructor initializer list.

Therefore, the declaration order must be maintained carefully whenever
components depend on previously created members.

---

## Dependency Injection

`GatewayApplication` acts as the composition root.

It creates the concrete implementations and injects them into the
components that require them.

Conceptually:

```text
Concrete Implementations
        │
        ▼
GatewayApplication
        │
        ├── IConnector
        ├── ISparkplugEncoder
        └── IMqttPublisher
```

The application lifecycle is therefore separated from the implementation
details of individual components.

---

## Multiple Device Handling

The gateway can integrate multiple industrial devices through different
connectors.

```text
                    GatewayApplication
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   ESP32Connector    OPCUAConnector   ModbusConnector
          │                │                │
          ▼                ▼                ▼
     DeviceData       DeviceData       DeviceData
          │                │                │
          └────────────────┼────────────────┘
                           ▼
                    SparkplugEncoder
                           │
                           ▼
                    MQTTPublisher
```

Each connector represents one source or device integration.

Each connector produces its own `DeviceData`.

Connectors do not merge their data into a shared `DeviceData` object.

Each `DeviceData` is processed and published independently.

---

## Initialization Sequence

During startup the application performs the following steps:

```text
Create Configuration
        │
        ▼
Create Connectors
        │
        ▼
Create SparkplugEncoder
        │
        ▼
Create MQTTPublisher
        │
        ▼
Initialize Connectors
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

The concrete connectors may represent different source protocols.

For example:

- ESP32Connector
- OPCUAConnector
- ModbusConnector

After successful initialization, the gateway enters its normal operating
mode.

---

## Main Execution Loop

During normal operation, the gateway continuously processes each connector.

Conceptually:

```text
IConnector
    │
collectData()
    │
    ▼
DeviceData
    │
    ▼
ISparkplugEncoder
    │
encode(DDATA)
    │
    ▼
SparkplugPayload
    │
    ▼
IMqttPublisher
    │
publish()
    │
    ▼
MQTT Broker
```

For multiple connectors:

```text
for each connector

    DeviceData = connector.collectData()

    SparkplugPayload =
        sparkplugEncoder.encode(DeviceData, DDATA)

    mqttPublisher.publish(SparkplugPayload)
```

Each connector produces its own `DeviceData`, which is encoded and
published independently.

The connectors do not directly communicate with one another.

---

## Sparkplug Publication Lifecycle

The gateway follows the Sparkplug lifecycle for the Edge Node.

```text
Startup
   │
   ▼
SparkplugEncoder
        │
encode(NDEATH)
        │
        ▼
SparkplugPayload
        │
        ▼
MQTTPublisher.connect()
        │
        ▼
MQTT Broker
        │
Connection established
        │
        ▼
SparkplugEncoder
        │
encode(NBIRTH)
        │
        ▼
MQTTPublisher.publish()
```

The NDEATH message is registered as the MQTT Last Will before the
connection to the broker is established.

After a successful connection, the gateway publishes NBIRTH.

During normal operation, device data is published using DDATA.

---

## Shutdown Sequence

A graceful shutdown performs the following operations:

```text
Publish DDEATH
        │
        ▼
Disconnect MQTT
        │
        ▼
Terminate Application
```

If the gateway terminates unexpectedly, the MQTT broker automatically
publishes the previously registered NDEATH message.

---

## Separation of Responsibilities

`GatewayApplication` coordinates the components but does not implement
their internal responsibilities.

```text
GatewayApplication
        │
        ├── IConnector
        │      └── Device acquisition
        │
        ├── ISparkplugEncoder
        │      └── Sparkplug B encoding
        │
        └── IMqttPublisher
               └── MQTT transport
```

This keeps the composition root independent from hardware and protocol
implementation details.

---

## Design Principles

- Composition Root
- Dependency Injection
- Programming to Interfaces
- Separation of Responsibilities
- Explicit Application Lifecycle
- Single Responsibility Principle
- Hardware and Protocol Independence

---

## Future Extensions

Future versions may introduce additional components such as:

- Logger
- Configuration Loader
- Database Storage
- Web Server
- OTA Update Manager
- Health Monitoring

Additional machine connectors can also be integrated without modifying the
core application flow.

Examples:

- ESP32Connector
- OPCUAConnector
- ModbusConnector
- RESTConnector

Each connector implements `IConnector` and produces the common
`DeviceData` representation.

The existing gateway processing pipeline remains unchanged.
