# MQTT Publisher

## Purpose

The `MQTTPublisher` is responsible for MQTT communication between the
Industrial Edge Gateway and the MQTT Broker.

It acts purely as a transport component.

The publisher does not know how Sparkplug messages are created and never
interprets their contents.

The `MQTTPublisher` belongs exclusively to the Industrial Edge Gateway.

---

## Responsibilities

The `MQTTPublisher`

- establishes the MQTT connection
- configures the MQTT Last Will
- publishes encoded messages
- disconnects gracefully
- reports connection status
- manages the MQTT transport session

The publisher never

- reads sensors
- acquires machine data
- creates metrics
- creates `DeviceData`
- encodes Sparkplug messages
- modifies application data
- accesses databases
- performs application-level processing

---

## Position in the Architecture

```text
Source Devices
      │
      ▼
  IConnector
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

The publisher represents the transport boundary between the Industrial
Edge Gateway and the MQTT Broker.

---

## Public Interface

```cpp
class IMqttPublisher
{
public:

    virtual ~IMqttPublisher() = default;

    virtual bool connect(const SparkplugPayload& willMessage) = 0;

    virtual void disconnect() = 0;

    virtual bool publish(const SparkplugPayload& payload) = 0;

    virtual bool isConnected() const = 0;
};
```

The interface separates the Gateway application from the concrete MQTT
client implementation.

It also allows the concrete MQTT implementation to be substituted without
modifying the surrounding gateway architecture.

This is useful, for example, for

- testing `GatewayApplication` with a mock publisher;
- replacing the underlying MQTT client library;
- testing connection and publishing logic without requiring a live MQTT
  broker.

---

## MQTTPublisher

```cpp
class MQTTPublisher : public IMqttPublisher
{
public:

    explicit MQTTPublisher(Configuration& configuration);

    bool connect(const SparkplugPayload& willMessage) override;

    void disconnect() override;

    bool publish(const SparkplugPayload& payload) override;

    bool isConnected() const override;

private:

    Configuration& configuration_;

    bool connected_;

    void configureWill(const SparkplugPayload& willMessage);

    bool reconnect();
};
```

The publisher receives its runtime configuration through dependency
injection.

The configuration contains MQTT connection parameters required by the
Gateway.

---

## Connection Lifecycle

The MQTT Last Will must be configured before the MQTT connection is
established.

The `GatewayApplication` coordinates the startup sequence.

```text
GatewayApplication
        │
        ▼
SparkplugEncoder
        │
        │ encode(NDEATH)
        ▼
SparkplugPayload
   (Last Will)
        │
        ▼
MQTTPublisher.connect()
        │
        ▼
   MQTT Broker
        │
        │ connection established
        ▼
SparkplugEncoder
        │
        │ encode(NBIRTH)
        ▼
MQTTPublisher.publish()
```

The `SparkplugEncoder` creates the Sparkplug-specific NDEATH message.

The `MQTTPublisher` registers this message as the MQTT Last Will during
connection establishment.

If the Gateway disconnects unexpectedly, the MQTT Broker automatically
publishes the configured Last Will message.

---

## Publishing

The publisher receives a fully encoded `SparkplugPayload`.

```cpp
struct SparkplugPayload
{
    MqttTopic topic;
    BinaryPayload payload;
};
```

The publisher

- does not interpret the topic
- does not interpret the payload
- does not modify the message
- does not perform Sparkplug encoding

It only transfers the provided topic and binary payload to the MQTT Broker.

```text
SparkplugPayload
      │
      ▼
MQTTPublisher
      │
      │ MQTT transport
      ▼
MQTT Broker
```

---

## Separation of Responsibilities

The boundary between Sparkplug encoding and MQTT transport is explicit.

```text
SparkplugEncoder
──────────────────────────────
Sparkplug knowledge
- Sparkplug topics
- Sparkplug message types
- Sparkplug metrics
- Protobuf payload
- seq
- bdSeq
- NDEATH content

            │
            ▼

SparkplugPayload

            │
            ▼

MQTTPublisher
──────────────────────────────
MQTT knowledge
- broker connection
- MQTT session
- Last Will registration
- publication
- connection status
- reconnect handling
```

Neither component performs the other's responsibilities.

---

## Design Principles

### Single Responsibility Principle

The `MQTTPublisher` is responsible only for MQTT transport.

### Transport Abstraction

The `IMqttPublisher` interface separates the Gateway application from the
concrete MQTT implementation.

The abstraction is justified by concrete testing and implementation
substitution requirements rather than by abstraction alone.

### Dependency Injection

The publisher receives its runtime configuration through its constructor.

### Protocol Separation

Sparkplug message creation is handled by the `SparkplugEncoder`.

MQTT transmission is handled by the `MQTTPublisher`.

### Hardware Independence

The publisher has no knowledge of sensors, PLCs, robots or other source
devices.

### No Application Logic

The publisher does not process measurements or make application-level
decisions.

---

## Future Extensions

Future versions may support

- automatic reconnect
- username/password authentication
- certificate-based authentication
- configurable QoS
- retained messages
- asynchronous publishing
- MQTT session configuration

These features can be added without changing the responsibilities of the
Gateway's data acquisition and encoding layers.

---

## Summary

The `MQTTPublisher` forms the MQTT transport layer of the Industrial Edge
Gateway.

It receives already encoded `SparkplugPayload` objects and transports them
to the MQTT Broker without interpreting their contents.

```text
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

The central architectural rule is:

The `SparkplugEncoder` defines what the message means;
the `MQTTPublisher` defines how the message is transported.
