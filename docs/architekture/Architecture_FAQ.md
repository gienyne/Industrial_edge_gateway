# Architecture FAQ & Design Clarifications

## Purpose

This document records the main questions, misunderstandings and architectural clarifications that arose while understanding the Industrial Edge Gateway.

The goal is not to replace the formal architecture documentation.

Instead, this document explains **why the architecture is structured this way** and answers questions that a developer discovering the project for the first time may naturally ask.

The central idea is:

> **Source devices acquire data. The Gateway standardizes that data. Sparkplug B standardizes the communication of that data.**

---

# 1. What is the overall data flow?

The complete architecture can be understood as a sequence of transformations:

```text
Physical Sensors
      │
      ▼
SensorReading
      │
      ▼
SensorConnector
      │
      ▼
Source Data
      │
      │ Raw Transport
      ▼
ESP32Connector
      │
      ▼
Metric
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

Each step has a different responsibility.

The important architectural boundary is between the **source device** and the **Industrial Edge Gateway**:

```text
SOURCE DEVICE                         EDGE GATEWAY

Sensor
   │
   ▼
SensorReading
   │
   ▼
SensorConnector
   │
   ▼
Raw Transport
   │
════════════════════════════════════════
             GATEWAY BOUNDARY
════════════════════════════════════════
   │
   ▼
ESP32Connector
   │
   ▼
Metric
   │
   ▼
DeviceData
   │
   ▼
SparkplugPayload
   │
   ▼
MQTT
```

---

# 2. Is the data produced by a sensor already a `Metric`?

**No.**

This is one of the most important distinctions in the architecture.

A physical sensor produces a **source-specific reading**.

For example, the DHT11 measures temperature and humidity.

Its source-side representation may be:

```text
DHT11Reading
├── temperature
├── humidity
├── timestamp
└── valid
```

The shock sensor may produce:

```text
ShockReading
├── detected
├── timestamp
└── valid
```

The light sensor may produce:

```text
LightReading
├── intensity
├── timestamp
└── valid
```

These are different structures because the physical sensors provide different information.

A `Metric`, on the other hand, is the **common representation used inside the Gateway**.

For example:

```text
Metric
├── name
├── datatype
├── value
├── unit
└── timestamp
```

The Gateway could therefore transform:

```text
DHT11Reading
temperature = 24.8
```

into:

```text
Metric
name = Temperature
value = 24.8
unit = °C
timestamp = ...
```

And:

```text
DHT11Reading
humidity = 56
```

into:

```text
Metric
name = Humidity
value = 56
unit = %
timestamp = ...
```

So:

```text
SensorReading ≠ Metric
```

The `SensorReading` is specific to the source.

The `Metric` is standardized for the Gateway.

---

# 3. Is the `SensorConnector` responsible for creating `Metric` objects?

**No.**

There are two different connectors in the architecture, and their names can be confusing.

## Source-side `SensorConnector`

The `SensorConnector` lives **inside the ESP32**.

Its job is to collect readings from the sensors.

For example:

```text
DHT11Sensor ──► DHT11Reading
ShockSensor ─► ShockReading
LightSensor ─► LightReading
```

The `SensorConnector` collects them:

```text
             SensorConnector
              /      |      \
             ▼       ▼       ▼
        DHT11Reading ShockReading LightReading
```

It then prepares them for transmission.

It does **not** convert them into `Metric`.

Therefore:

```text
Sensor
  ↓
SensorReading
  ↓
SensorConnector
  ↓
SourceData
  ↓
Raw Transport
```

---

# 4. Then what does the Gateway `ESP32Connector` do?

The `ESP32Connector` lives **inside the Industrial Edge Gateway**.

It understands how to interpret the data coming from the ESP32.

It performs the conversion:

```text
Source-specific data
        │
        ▼
ESP32Connector
        │
        ▼
Metric
```

For example:

```text
DHT11Reading
temperature = 24.8
```

can become:

```text
Metric
name = Temperature
value = 24.8
unit = °C
timestamp = ...
```

And:

```text
ShockReading
detected = false
```

can become:

```text
Metric
name = Shock
value = false
unit = ...
timestamp = ...
```

The same principle applies to future connectors:

```text
OPCUA data
    ↓
OPCUAConnector
    ↓
Metric
```

or:

```text
Modbus data
    ↓
ModbusConnector
    ↓
Metric
```

This is where the source-specific representation becomes the common Gateway representation.

---

# 5. Does this mean that `SensorConnector` is useless?

**No.**

It has a different job.

Think about a restaurant.

The individual sensors are the cooks:

```text
DHT11Sensor
ShockSensor
LightSensor
```

Each cook knows how to prepare one particular thing.

The `SensorConnector` is the person who collects the dishes from all cooks:

```text
DHT11Reading
ShockReading
LightReading
```

and puts them together so they can be sent as one source-device data package.

It does not redesign the dishes.

It simply collects and prepares them for transport.

Therefore the source-side architecture is:

```text
DHT11Sensor ──► DHT11Reading ──┐
ShockSensor ─► ShockReading ───┼──► SensorConnector
LightSensor ─► LightReading ───┘
                                      │
                                      ▼
                                  SourceData
                                      │
                                      ▼
                                Raw Transport
```

The actual standardization happens later:

```text
SourceData
    ↓
ESP32Connector
    ↓
Metric
```

So the `SensorConnector` is useful because it separates:

* sensor hardware access;
* local sensor aggregation;
* source-device transport;
* Gateway-side data standardization.

---

# 6. What does "common internal data model" mean?

The **common internal data model** is simply the format that the Gateway uses internally regardless of where the data came from.

Imagine that several people speak different languages:

```text
ESP32          → source-specific format
OPC UA machine → OPC UA representation
Modbus machine → Modbus representation
REST device    → REST representation
```

The Gateway does not want its entire software to speak all of these languages.

Instead, each connector translates its source into one common language:

```text
ESP32 ────────► ESP32Connector ────┐
OPC UA ───────► OPCUAConnector ────┤
Modbus ───────► ModbusConnector ───┤
REST ─────────► RESTConnector ─────┘
                                   │
                                   ▼
                             Common Model
                             Metric
                               │
                               ▼
                           DeviceData
```

That common model is:

```text
Metric
DeviceData
```

This is what the documentation means by:

> Source-specific data is converted into a common gateway representation.

---

# 7. What exactly is a `Metric`?

A `Metric` represents **one measurement**.

For example:

```text
Temperature = 24.8 °C
```

is one metric.

```text
Humidity = 56 %
```

is another metric.

```text
MotorSpeed = 1500 rpm
```

is another metric.

Conceptually:

```text
Metric
├── name
├── datatype
├── value
├── unit
└── timestamp
```

---

# 8. But couldn't different machines use different names?

Yes.

This is an important point.

Simply having:

```text
name
value
unit
```

does not automatically guarantee semantic standardization.

For example:

```text
Machine A:
name = Temperature

Machine B:
name = Temperatur

Machine C:
name = Temp
```

All three could technically represent the same physical concept.

Therefore, there are two different ideas:

### Structural standardization

The Gateway guarantees that measurements have a common structure:

```text
name
value
unit
timestamp
```

### Semantic standardization

The project may also need conventions for names, units, datatypes and meanings.

For example, the project could decide that temperature measurements should use:

```text
name = Temperature
unit = °C
datatype = double
```

Then connectors must translate their source-specific terminology into that convention.

This distinction is important because **Sparkplug B alone does not magically decide what every application-specific metric name should mean**.

The project may need its own metric naming and semantic conventions in addition to using Sparkplug B.

---

# 9. What is `DeviceData`?

A `Metric` represents one measurement.

`DeviceData` groups the metrics belonging to one physical device.

For example:

```text
DeviceData
deviceId = ESP32-01

metrics:
    Temperature = 24.8 °C
    Humidity = 56 %
    Shock = false
    Light = 820
```

Conceptually:

```text
DeviceData
├── deviceId
├── metrics
└── timestamp
```

Another machine could produce:

```text
DeviceData
deviceId = Robot-01

metrics:
    Temperature = 42 °C
    MotorCurrent = 4.2 A
    Speed = 1500 rpm
    Alarm = false
```

The Gateway can process both in the same way.

---

# 10. Why not simply use `Metric` everywhere?

Because `Metric` and `DeviceData` represent different levels.

Think of a school.

A single student is like:

```text
Metric
```

A class containing several students is like:

```text
DeviceData
```

For example:

```text
DeviceData: ESP32-01
│
├── Metric: Temperature
├── Metric: Humidity
├── Metric: Shock
└── Metric: Light
```

The Gateway therefore has a hierarchy:

```text
DeviceData
     │
     ├── Metric
     ├── Metric
     ├── Metric
     └── Metric
```

---

# 11. Why are `SensorReading` structures different?

Because they describe the **physical source**, not the Gateway.

For example:

```text
DHT11Reading
├── temperature
├── humidity
├── timestamp
└── valid
```

The DHT11 naturally gives two measurements together.

The shock sensor gives:

```text
ShockReading
├── detected
├── timestamp
└── valid
```

The light sensor gives:

```text
LightReading
├── intensity
├── timestamp
└── valid
```

These structures are allowed to be different because they are hardware-specific.

The Gateway does not want to depend on these structures.

It wants:

```text
Metric
```

and:

```text
DeviceData
```

instead.

---

# 12. Why not make the ESP32 directly produce `Metric`?

It could be done technically, but it would create stronger coupling.

The architecture intentionally says:

```text
ESP32
   │
   └── source-specific representation
```

and:

```text
Gateway
   │
   └── common internal representation
```

This means the ESP32 does not need to know how the Gateway internally represents data.

This becomes especially useful when the Gateway later receives data from:

```text
ESP32
OPC UA
Modbus
REST
...
```

Each source can have its own format.

The connectors perform the translation.

---

# 13. What is Sparkplug B doing?

Sparkplug B is used at the **communication/publication layer**.

The Gateway first has:

```text
DeviceData
```

Then:

```text
DeviceData
    ↓
SparkplugEncoder
    ↓
SparkplugPayload
```

The `SparkplugEncoder` converts the Gateway's internal representation into a Sparkplug B representation.

Then:

```text
SparkplugPayload
    ↓
MQTTPublisher
    ↓
MQTT Broker
```

The important distinction is:

```text
Metric / DeviceData
        ↓
   Gateway model
```

while:

```text
SparkplugPayload
        ↓
 Communication model
```

The internal data model does not need to know about MQTT or Sparkplug B.

---

# 14. Does Sparkplug B standardize the actual meaning of every measurement?

Not completely.

Sparkplug B provides a standardized way of communicating industrial data using MQTT.

It defines things such as:

* topic structure;
* message types;
* metrics within payloads;
* datatypes;
* sequence information;
* birth/death state concepts.

But the project still needs to decide how application-specific measurements are named and interpreted.

For example, the project may establish conventions such as:

```text
Temperature → °C
Humidity    → %
MotorSpeed  → rpm
Current     → A
```

Therefore:

```text
Common Gateway Model
        +
Project metric conventions
        +
Sparkplug B
```

together provide a consistent system.

---

# 15. What is Dependency Injection?

Dependency Injection means:

> A component receives the things it needs instead of creating them itself.

Imagine a child building a LEGO car.

The child needs:

```text
Wheels
Engine
Body
```

A bad design would be:

```text
Car
 ├── creates wheels itself
 ├── creates engine itself
 └── creates body itself
```

A better design is:

```text
Parent
 ├── creates wheels
 ├── creates engine
 └── creates body
          │
          ▼
         Car
```

The car simply says:

> "I need wheels."

Someone else gives it the wheels.

---

# 16. What does Dependency Injection mean in this project?

For example, `MQTTPublisher` needs an MQTT client.

Instead of doing everything itself:

```text
MQTTPublisher
    │
    ├── creates MQTT client
    ├── chooses broker
    └── creates configuration
```

the application creates those things:

```text
GatewayApplication
       │
       ├── creates Configuration
       ├── creates MQTT client
       │
       └── gives them to MQTTPublisher
```

The publisher can then simply use what it received.

---

# 17. What is the "Composition Root"?

The **composition root** is simply the place where the application's components are assembled.

In this project:

```text
GatewayApplication
```

is the composition root.

It creates and connects:

```text
Configuration
Connectors
SparkplugEncoder
MQTTPublisher
```

Conceptually:

```text
GatewayApplication
       │
       ├── Configuration
       │
       ├── ESP32Connector
       │
       ├── OPCUAConnector
       │
       ├── ModbusConnector
       │
       ├── SparkplugEncoder
       │
       └── MQTTPublisher
```

It is like the person who assembles all the LEGO pieces before the machine starts.

---

# 18. Why is Dependency Injection useful?

Because components become easier to replace and test.

For example, in production:

```text
MQTTPublisher
      ↓
Real MQTT client
      ↓
MQTT Broker
```

During a test:

```text
MQTTPublisher
      ↓
Fake MQTT client
      ↓
Test environment
```

The `MQTTPublisher` does not need to change.

This makes the software easier to test and maintain.

---

# 19. What is `Configuration`?

`Configuration` stores settings needed by the Gateway.

For example:

```text
Broker Address
Broker Port
Client ID
Sparkplug Group ID
Sparkplug Edge Node ID
Publish Interval
```

It does **not** contain sensor measurements.

It is configuration, not data.

For example:

```text
Configuration
├── brokerAddress
├── brokerPort
├── clientId
├── groupId
├── edgeNodeId
└── publishInterval
```

while:

```text
DeviceData
├── deviceId
├── Temperature
├── Humidity
├── Shock
└── Light
```

are runtime data.

---

# 20. Why separate source configuration from Gateway configuration?

Because they belong to different systems.

The ESP32 may need:

```text
Sensor configuration
Sampling interval
GPIO configuration
Source transport settings
```

The Gateway may need:

```text
MQTT broker
Sparkplug group
Edge Node ID
Publish interval
```

These should not all be mixed together.

The architecture therefore separates:

```text
ESP32
└── Local configuration
```

from:

```text
Gateway
├── Gateway Configuration
└── Connector Configuration
```

---

# 21. What happens when we add OPC UA or Modbus?

The central pipeline should remain the same.

For example:

```text
ESP32
   ↓
ESP32Connector
   ↓
Metric
   ↓
DeviceData
   ↓
SparkplugEncoder
   ↓
MQTT
```

For OPC UA:

```text
OPC UA
   ↓
OPCUAConnector
   ↓
Metric
   ↓
DeviceData
   ↓
SparkplugEncoder
   ↓
MQTT
```

For Modbus:

```text
Modbus
   ↓
ModbusConnector
   ↓
Metric
   ↓
DeviceData
   ↓
SparkplugEncoder
   ↓
MQTT
```

The middle and final parts remain unchanged.

This is one of the main reasons for having the common internal data model.

---

# 22. Where does the Database fit?

The Database is **not required for Sprint 1**.

Later, if historical storage is introduced, one possible architecture is:

```text
                         MQTT Broker
                              │
                     Sparkplug B messages
                              │
                              ▼
                    Database Ingestion
                         Service
                              │
                              ▼
                         Database
                              │
                              ▼
                         Dashboard
```

The Database Ingestion Service subscribes to MQTT, receives Sparkplug B messages and decodes them.

It then stores the useful information in a database-oriented structure.

For example:

```text
timestamp
device_id
metric_name
value
unit
```

---

# 23. Does the Database have to store Sparkplug B?

**Not necessarily.**

Sparkplug B is primarily a communication representation.

A database is a storage system.

Therefore, a clean architecture can be:

```text
MQTT Broker
     │
     │ Sparkplug B
     ▼
Database Ingestion Service
     │
     │ decoded/structured data
     ▼
Database
```

The service understands Sparkplug B.

The database does not necessarily need to.

For example, the database could store:

```text
device_id = ESP32-01
metric = Temperature
value = 24.8
unit = °C
timestamp = ...
```

instead of storing the complete Sparkplug B payload.

This generally makes querying historical data easier.

---

# 24. Does the Dashboard have to understand Sparkplug B?

**No.**

There are several possible architectures.

### Database-oriented dashboard

```text
MQTT
 │
 ▼
Database Ingestion
 │
 ▼
Database
 │
 ▼
Dashboard
```

The Dashboard asks the database:

> Give me the temperature of ESP32-01 for the last 24 hours.

This is usually convenient for historical visualization.

### MQTT-oriented dashboard

Alternatively:

```text
MQTT Broker
     │
     ▼
Dashboard
```

Here the Dashboard consumes Sparkplug B directly and therefore needs Sparkplug B support.

### Hybrid architecture

A future system could also use both:

```text
                         MQTT Broker
                        /           \
                       ▼             ▼
             Database Ingestion   Dashboard
                       │             │
                       ▼             │
                    Database ◄───────┘
```

The Dashboard could use MQTT for real-time data and the database for historical data.

---

# 25. Which approach is recommended for this project?

For the Industrial Edge Gateway, a good future architecture would be:

```text
                  Industrial Edge Gateway
                           │
                           │ Sparkplug B / MQTT
                           ▼
                      MQTT Broker
                           │
                 ┌─────────┴─────────┐
                 │                   │
                 ▼                   ▼
        Database Ingestion        Real-time
             Service             Consumers
                 │
                 ▼
             Database
                 │
                 ▼
             Dashboard
```

The reasoning is:

### MQTT + Sparkplug B

Used for standardized industrial communication.

### Database

Used for persistent historical storage and efficient querying.

### Database Ingestion Service

Responsible for understanding Sparkplug B and converting the messages into a form appropriate for storage.

### Dashboard

Can query the database for historical information without needing to understand the complete Sparkplug B protocol.

This keeps responsibilities separated.

---

# 26. Does this mean Sparkplug B is useless once the data enters the Database?

No.

Sparkplug B remains very useful for **interoperability between systems**.

For example:

```text
Industrial Gateway
        │
        │ Sparkplug B
        ▼
    MQTT Broker
        │
        ├──► Database Service
        ├──► SCADA
        ├──► Monitoring System
        └──► Other MQTT clients
```

Different systems can understand the standardized communication without needing to know the internal implementation of the Gateway.

The database has a different purpose:

```text
Sparkplug B
   → communication / interoperability

Database
   → storage / querying / history
```

---

# 27. Why not put the Database directly inside the Gateway?

It is technically possible, but it would increase the Gateway's responsibilities.

The Gateway would then have to handle:

```text
Data acquisition
+
Data transformation
+
Sparkplug
+
MQTT
+
Database
+
Database connection management
```

The current architecture deliberately keeps these responsibilities separate.

A future database component can consume the standardized output without modifying the central Gateway pipeline.

This follows the same modular philosophy as the connectors.

---

# 28. The central architectural idea

The project can ultimately be understood as three major layers.

## Layer 1 — Acquisition

The source knows how to obtain data.

```text
Sensors
   ↓
SensorReading
   ↓
SensorConnector
```

This layer is hardware-specific.

---

## Layer 2 — Gateway normalization

The Gateway converts different sources into one internal representation.

```text
ESP32Connector
OPCUAConnector
ModbusConnector
RESTConnector
        │
        ▼
     Metric
        │
        ▼
   DeviceData
```

This layer is hardware- and protocol-independent.

---

## Layer 3 — Standardized communication

The Gateway converts the internal model into Sparkplug B and publishes it through MQTT.

```text
DeviceData
    ↓
SparkplugEncoder
    ↓
SparkplugPayload
    ↓
MQTTPublisher
    ↓
MQTT Broker
```

This layer is responsible for external communication.

---

# 29. The complete mental model

If you remember only one diagram, remember this:

```text
                 SOURCE DEVICE
                      │
          ┌───────────┴───────────┐
          │                       │
       Sensors               Source logic
          │
          ▼
    SensorReading
          │
          ▼
    SensorConnector
          │
          ▼
      Raw Data
          │
          │
══════════╪════════════════════════════════
          │        GATEWAY BOUNDARY
          ▼
    Source Connector
          │
          ▼
       Metric
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
          │
       ┌──┴───────────────┐
       │                  │
       ▼                  ▼
 Database Service     Other Clients
       │
       ▼
   Database
       │
       ▼
   Dashboard
```

---

# 30. Key terms at a glance

| Concept              | Meaning                                              |
| -------------------- | ---------------------------------------------------- |
| `Sensor`             | Talks directly to physical sensor hardware           |
| `SensorReading`      | Hardware/source-specific measurement                 |
| `SensorConnector`    | Collects sensor readings on the source device        |
| `SourceData`         | Data prepared by the source for transport            |
| `IConnector`         | Gateway abstraction for source integrations          |
| `ESP32Connector`     | Converts ESP32 source data into Gateway data         |
| `Metric`             | One standardized measurement inside the Gateway      |
| `DeviceData`         | Collection of metrics belonging to one device        |
| `SparkplugEncoder`   | Converts Gateway data into Sparkplug representation  |
| `SparkplugPayload`   | Data ready for Sparkplug/MQTT publication            |
| `MQTTPublisher`      | Handles MQTT transport                               |
| `Configuration`      | Gateway runtime settings                             |
| `GatewayApplication` | Creates and connects the Gateway components          |
| MQTT Broker          | Routes MQTT messages between producers and consumers |
| Database             | Stores historical/queryable data                     |
| Dashboard            | Visualizes data                                      |

---

# 31. Final principles

The most important rules of the architecture are:

### Rule 1

**Sensors know about hardware, not the Gateway.**

```text
Sensor → SensorReading
```

### Rule 2

**The source-side `SensorConnector` collects data but does not create Gateway `Metric` objects.**

```text
SensorReading → SensorConnector → SourceData
```

### Rule 3

**Gateway connectors translate source-specific data into the common Gateway model.**

```text
SourceData → Connector → Metric
```

### Rule 4

**`Metric` represents one standardized measurement.**

```text
Metric = one measurement
```

### Rule 5

**`DeviceData` groups the metrics belonging to one physical device.**

```text
DeviceData
 ├── Metric
 ├── Metric
 └── Metric
```

### Rule 6

**Sparkplug B is introduced by the Gateway, not by the ESP32.**

```text
DeviceData → SparkplugEncoder → Sparkplug B
```

### Rule 7

**MQTT is the transport mechanism; Sparkplug B defines the standardized industrial messaging model carried over MQTT.**

### Rule 8

**The Database does not have to store Sparkplug B payloads.**

A service can decode Sparkplug B before storing the data.

### Rule 9

**The Dashboard does not have to understand Sparkplug B if it obtains historical data from the Database.**

### Rule 10

**New source technologies should normally require a new connector, not changes to the central processing pipeline.**

```text
New Source
    ↓
New Connector
    ↓
Metric
    ↓
DeviceData
    ↓
Existing Sparkplug pipeline
```

This is the main idea behind the modular architecture.
