# Project Roadmap

The project is developed incrementally.

---

# Sprint 1

## Goal

Create the first complete Sparkplug communication chain.

Sensor

↓

ESP32

↓

Sparkplug B

↓

MQTT Broker

↓

MQTT Client

### Sensors

- KY-015 DHT11
- KY-002 Shock Sensor
- KY-024 Hall Sensor

### Objectives

- read sensor values
- encode Sparkplug messages
- publish via MQTT
- verify received messages

No database.

No dashboard.

---

# Sprint 2

Introduce the Gateway application.

Responsibilities

- receive Sparkplug messages
- validate messages
- logging
- architecture improvements

---

# Sprint 3

Database integration

- InfluxDB
- Metadata database

---

# Sprint 4

Dashboard integration

- Grafana
or
- custom dashboard

---

# Sprint 5

Industrial machine integration

Possible protocols

- OPC UA
- Modbus TCP
- REST
- other protocols

The gateway architecture should require only a new connector while the
remaining software stays unchanged.

---

# Sprint 6

Possible Bachelor extensions

Examples

- Predictive Maintenance
- Anomaly Detection
- Sparkplug performance evaluation
- Comparison with OPC UA PubSub