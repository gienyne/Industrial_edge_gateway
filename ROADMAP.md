# Project Roadmap

The Industrial Edge Gateway is developed through incremental sprints.

Each sprint adds new capabilities while preserving the existing architecture.
The common internal data model and connector-based architecture remain the
stable foundation throughout the project.

---

# Sprint 1 — Foundation

## Goal

Build and validate the first end-to-end communication chain between an
ESP32 source device and the Industrial Edge Gateway.

### Scope

**Source Device (ESP32)**

- KY-015 DHT11
- KY-002 Shock Sensor
- KY-018 Photoresistor
- Sensor acquisition
- SensorConnector
- MQTT source communication

**Industrial Edge Gateway**

- ESP32Connector
- Metric
- DeviceData
- SparkplugEncoder
- MQTTPublisher
- Sparkplug B publication

### Target Architecture

```text
ESP32
   │
   │ MQTT
   ▼
MQTT Broker
   │
   │ MQTT
   ▼
ESP32Connector
   │
   ▼
DeviceData
   │
   ▼
SparkplugEncoder
   │
   ▼
MQTTPublisher
   │
   ▼
MQTT Broker
```

Sparkplug B is introduced only at the Gateway level. The ESP32 remains
independent of the Sparkplug representation.

### Deliverables

- acquire sensor measurements;
- publish source data through MQTT;
- convert source data into `DeviceData`;
- generate valid Sparkplug B messages;
- publish `NBIRTH` and `DDATA`;
- validate the complete communication chain.

### Out of Scope

- databases;
- dashboards;
- industrial machines;
- multi-source integration.

---

# Sprint 2 — Multi-Source Gateway

## Goal

Validate the connector-based architecture with multiple heterogeneous
source types.

### Planned Features

- support multiple `IConnector` implementations;
- integrate a second source using a different communication protocol;
- improve connector lifecycle management;
- load connector configuration from an external configuration source where
  appropriate;
- strengthen diagnostics and error handling.

### Second Data Source

The second source will be a simulated OPC UA server, such as a simulation
server or a small custom server.

It will expose a small set of synthetic industrial variables, for example:

- temperature;
- motor speed;
- alarm status.

The simulated source is not connected to real laboratory equipment.

The purpose is to validate the `IConnector` abstraction against a genuinely
different communication technology before integrating real industrial
equipment.

Integration with real Smart Factory equipment remains the dedicated goal
of Sprint 5.

### Expected Architecture

```text
ESP32 ─────────┐
               │
               │
OPC UA ────────┤
(simulated)    ▼
        Gateway Connectors
               │
               ▼
           DeviceData
               │
               ▼
        SparkplugEncoder
               │
               ▼
         MQTTPublisher
```

The common Gateway processing pipeline remains independent of the source
technology.

---

# Sprint 3 — Data Persistence

## Goal

Introduce persistence for standardized industrial data without coupling
the Gateway core to a specific source protocol.

### Planned Capabilities

- time-series data storage;
- metadata storage;
- historical device measurements;
- data retention strategy.

The persistence layer consumes standardized Gateway data rather than
source-specific formats.

The exact database or storage technology remains subject to later
evaluation.

---

# Sprint 4 — Backend & Visualization

## Goal

Provide monitoring and visualization for standardized industrial data.

### Planned Components

- Spring Boot backend;
- REST API;
- Grafana or custom dashboard;
- historical visualization.

The backend consumes standardized Gateway data and remains independent from
individual machine protocols.

---

# Sprint 5 — Smart Factory Integration

## Goal

Integrate real industrial equipment into the Industrial Edge Gateway.

### Target Equipment

- CNC machine;
- collaborative robots;
- injection molding machine;
- additional Smart Factory systems.

### Planned Protocols

- OPC UA;
- Modbus TCP;
- REST APIs;
- vendor-specific interfaces.

New source integrations should be implemented through dedicated connector
implementations.

The Gateway core should not require source-specific changes for each new
integration.

---

# Sprint 6 — Advanced Industrial Features

## Goal

Extend the platform with research-oriented capabilities.

### Possible Extensions

- Predictive Maintenance;
- Anomaly Detection;
- Sparkplug performance evaluation;
- comparison with OPC UA PubSub;
- Gateway scalability analysis.

The exact Bachelor Thesis topic will be selected later based on the results
of the previous sprints.

---

# Long-Term Vision

The intended architecture of the completed platform is:

```text
Multiple Source Devices
          │
          ▼
     Gateway Connectors
          │
          ▼
       DeviceData
          │
          ▼
    SparkplugEncoder
          │
          ▼
     MQTTPublisher
          │
          ▼
      MQTT Broker
          │
          ▼
Backend • Database • Dashboard
```

New source types are intended to be integrated through dedicated connector
implementations while the common Gateway processing pipeline remains
independent of the source technology.
