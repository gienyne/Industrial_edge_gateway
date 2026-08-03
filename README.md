# Industrial Edge Gateway using Sparkplug B

## Overview

This project is developed as part of the Smart Factory Mittelhessen.

The objective is to design a modular Industrial Edge Gateway capable of collecting data from heterogeneous sources, converting them into the Sparkplug B format and publishing them through MQTT.

The development follows an incremental approach. The first prototype uses an ESP32 and a small set of sensors to validate the complete Sparkplug communication chain. Once this foundation is stable, the same architecture will be extended to industrial machines using protocols such as OPC UA, Modbus TCP or other communication interfaces.

---

## Objectives

The gateway shall

- acquire data from embedded sensors during the first development phase;
- later acquire data from industrial machines using different communication protocols;
- represent all acquired data using the Sparkplug B specification;
- publish standardized messages via MQTT;
- provide an architecture that can be extended by adding new machine connectors instead of modifying the existing software;
- support future integration with databases and visualization systems.

---

## Development Strategy

The project is developed incrementally.

Each sprint introduces one architectural building block while preserving the existing implementation.

This approach allows the gateway to evolve from a laboratory prototype into an industrial gateway without major architectural changes.

---

## Repository Structure

See the repository directories for the firmware, gateway, backend, database, documentation and future extensions.

The complete project roadmap is available in **ROADMAP.md**.

Development rules are described in **CONTRIBUTING.md**.
