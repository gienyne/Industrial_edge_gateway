# Project Roadmap

The project is implemented in several incremental development sprints.

Each sprint extends the existing architecture instead of replacing it.

---

# Sprint 1

## Goal

Build the first complete Sparkplug B communication chain using embedded sensors.

---

## Hardware

- ESP32
- KY-015 DHT11 Temperature Sensor
- KY-002 Shock Sensor
- KY-018 Photoresistor

---

## System Architecture

```
KY-015      KY-002      KY-018
    │           │           │
    └────────── ESP32 ──────────┘
                 │
         Sensor Acquisition
                 │
        Internal Sensor Data
                 │
        Sparkplug B Encoder
                 │
          MQTT Publisher
                 │
          Public MQTT Broker
                 │
         MQTT Explorer / Client
```

---

## Deliverables

- acquire sensor measurements;
- create valid Sparkplug B messages;
- publish messages via MQTT;
- subscribe and verify transmitted messages;
- validate the complete communication chain.

---

## Out of Scope

- databases;
- backend application;
- dashboard;
- industrial machines.

---

# Sprint 2

Introduce the Java Gateway.

Responsibilities:

- receive Sparkplug messages;
- validate incoming data;
- improve software architecture;
- prepare interfaces for future machine connectors.

---

# Sprint 3

Database integration.

Planned components:

- InfluxDB (time-series data);
- metadata database.

---

# Sprint 4

Backend and visualization.

Possible components:

- Spring Boot backend;
- Grafana or custom dashboard.

---

# Sprint 5

Industrial machine integration.

Possible communication protocols:

- OPC UA;
- Modbus TCP;
- REST APIs;
- vendor-specific interfaces.

Only new connectors should be added. The gateway core should remain unchanged.

---

# Sprint 6

Possible Bachelor Thesis extensions.

Examples:

- Predictive Maintenance;
- Anomaly Detection;
- Sparkplug performance evaluation;
- Comparison with OPC UA PubSub.
