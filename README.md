# Industrial Edge Gateway using Sparkplug B

## Overview

This project is developed as part of the Smart Factory Mittelhessen.

The objective is to design a modular Industrial Edge Gateway capable of
integrating heterogeneous source devices and industrial systems through
dedicated connectors.

Source-specific data is converted into a common internal data model and
then encoded as Sparkplug B before being published through MQTT.

The first prototype uses an ESP32 with a small set of sensors. The ESP32
acts as a source device and communicates its data to the Gateway through
MQTT. Sparkplug B is implemented exclusively at the Gateway level.

The architecture is intended to support future integration of industrial
machines using communication technologies such as OPC UA, Modbus TCP and
REST APIs.

---

## Objectives

The Gateway shall

- integrate data from heterogeneous source devices;
- isolate source-specific communication through connector implementations;
- convert source data into a common internal data model;
- encode standardized Gateway data as Sparkplug B;
- publish Sparkplug B messages through MQTT;
- support multiple source devices without coupling the Gateway core to a
  specific source technology;
- allow future integration with persistence, backend and visualization
  components.

---

## Architecture

The Gateway follows a connector-based architecture:

```text
Source Devices / Industrial Systems
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
```

Each connector is responsible for acquiring data from its source and
converting it into the common internal data model.

The Gateway core does not depend on the specific source technology.

Sparkplug B is introduced only at the Gateway output boundary. Source
devices do not need to implement Sparkplug B.

---

## Development Strategy

The project is developed incrementally through several development sprints.

Each sprint adds new capabilities while preserving the existing
architecture.

The common internal data model and connector-based architecture form the
stable foundation of the project.

The first sprint validates the complete data flow using an ESP32 and three
sensors. Later sprints extend the Gateway with additional source types,
data persistence, backend and visualization, and finally integration with
real Smart Factory equipment.

The complete development plan is available in `ROADMAP.md`.

---

## Current Prototype

The first prototype uses:

- ESP32 as the source device;
- KY-015 DHT11 temperature sensor;
- KY-002 shock sensor;
- KY-018 photoresistor;
- MQTT for source-to-Gateway communication;
- `ESP32Connector` for Gateway-side integration;
- `DeviceData` as the common internal representation;
- `SparkplugEncoder` for Sparkplug B encoding;
- `MQTTPublisher` for MQTT publication.

The ESP32 does not generate Sparkplug B messages. Standardization is
performed once, at the Gateway level.

Implementation of this prototype is in progress. See `ROADMAP.md` and
`docs/sprints/sprint_01.md` for the current development status.

---

## Repository Structure

The repository contains the ESP32 source-device firmware, the Industrial
Edge Gateway implementation, and the project documentation.

The source-device firmware and the Gateway are separate software
components with independent build environments, organized within this
repository.

Additional components such as backend, database and visualization will be
introduced as their corresponding development sprints are implemented.

---

## Future Development

The architecture is intended to support additional source types through
dedicated connector implementations.

Planned development includes:

- additional simulated source types;
- OPC UA, Modbus TCP and REST integrations;
- data persistence;
- backend services;
- visualization;
- integration with real Smart Factory equipment;
- possible research-oriented extensions for the Bachelor thesis.

The exact implementation of future components may evolve as the project
progresses.

---

## Documentation

Architectural decisions and design details are documented separately.

- `ROADMAP.md` -> development roadmap and sprint planning;
- `CONTRIBUTING.md` -> development and contribution rules;
- `docs/architecture/` -> architectural documentation and decisions.

The documentation is kept separate from the implementation so that
architectural decisions can be reviewed independently from the code.
