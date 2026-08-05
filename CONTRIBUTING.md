# Development Workflow

This document defines the development workflow and coding conventions used throughout the project.

The goal is to keep the codebase consistent, maintainable and easy to extend as the project evolves.

---

# Git Workflow

## Branches

### `main`

Contains only stable and tested releases.

---

### `develop`

Main integration branch.

Completed features are merged into **develop** before being released to **main**.

---

### `feature/<name>`

Each feature is developed in its own branch.

Examples:

```text
feature/sprint1-sensors
feature/sparkplug-encoder
feature/java-gateway
feature/influxdb
feature/dashboard
feature/opcua-connector
```

---

# Commit Convention

Keep commits small, focused and meaningful.

Examples:

```text
feat: add DHT11 driver
feat: implement Sparkplug encoder
feat: publish NBIRTH message
fix: correct MQTT topic
refactor: simplify sensor interface
docs: update architecture
```

---

# Coding Conventions

## Naming

### Classes

Use **PascalCase**.

Examples:

```text
Configuration
SensorConnector
SparkplugEncoder
MQTTPublisher
DeviceData
Metric
```

---

### Interfaces

Prefix every interface with **I**.

Examples:

```text
ISensor
IConnector
ISparkplugEncoder
IMQTTPublisher
```

---

### Methods

Use **camelCase**.

Examples:

```text
initialize()
collectData()
createMetric()
publish()
encode()
```

---

### Member Variables

Use **camelCase** followed by a trailing underscore (`_`).

Examples:

```text
configuration_
deviceId_
mqttBroker_
mqttPort_
publishInterval_
sensors_
```

This convention clearly distinguishes class members from local variables and constructor parameters.

---

### Function Parameters

Use **camelCase** without prefixes or suffixes.

Examples:

```text
configuration
sensor
reading
metric
payload
```

---

### Constants

Use **UPPER_CASE**.

Examples:

```text
SENSOR_COUNT
DEFAULT_MQTT_PORT
DEFAULT_PUBLISH_INTERVAL
```

---

# Design Rules

* Program against interfaces, not implementations.
* Follow the Single Responsibility Principle.
* Prefer dependency injection over global objects.
* Avoid dynamic memory allocation whenever possible.
* Keep hardware-specific code inside connectors and sensor drivers.
* Keep the gateway core independent of hardware and communication protocols.
* Extend the architecture instead of rewriting existing components.

---

# Documentation Rules

* Document every architectural decision.
* Update the documentation together with the implementation.
* Keep diagrams synchronized with the code.
* Record major design decisions using Architecture Decision Records (ADRs).

---

# General Rules

* Keep commits focused on a single change.
* Test before merging.
* Never commit generated files.
* Keep the project modular.
* Prefer clarity over cleverness.
* Build for future extensibility without introducing unnecessary complexity.
