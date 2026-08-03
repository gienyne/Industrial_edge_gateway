# Industrial Edge Gateway using Sparkplug B

## Overview

This project is developed as part of the Smart Factory Mittelhessen.

The goal is to design an extensible Industrial Edge Gateway that collects
sensor and machine data, converts them into the Sparkplug B format and
publishes them via MQTT.

The project starts with simple sensors connected to an ESP32.
Later, the same architecture will be extended to real industrial machines
using protocols such as OPC UA or Modbus.

---

## Project Goals

- collect data from different data sources
- standardize data using Sparkplug B
- publish data via MQTT
- build a modular gateway architecture
- integrate databases and dashboards
- support future industrial machine integration

---

## Development Strategy

Instead of starting directly with industrial machines, the project is
implemented incrementally.

Each sprint introduces one new architectural component while keeping
previous components unchanged.

This approach allows the gateway architecture to evolve naturally without
major redesigns.

---

## Repository Structure
