# ADR-002 — Centralized Gateway Architecture

## Status

Accepted

---

## Context

The Industrial Edge Gateway is intended to integrate data from multiple
heterogeneous source devices.

These devices may use different hardware platforms and communication
protocols, such as ESP32, OPC UA, Modbus or REST.

Placing protocol conversion and MQTT/Sparkplug handling on every source
device would duplicate functionality and make the system harder to maintain.

---

## Decision

The system shall use one centralized Industrial Edge Gateway to integrate
multiple source devices.

Source devices are responsible only for acquiring and exposing their data.
Gateway connectors handle source-specific communication and convert the
acquired data into the common internal data model.

The gateway performs protocol conversion, Sparkplug encoding and MQTT
communication centrally.

```text
Source Device 1 ──┐
Source Device 2 ──┤
Source Device 3 ──┼──► Industrial Edge Gateway ──► MQTT / Sparkplug B
      ... ─────────┤
Source Device N ──┘
```

The detailed architecture is documented in `overview.md` and the application
composition is documented in `gateway-application.md`.

---

## Consequences

### Advantages

- New machines can be integrated through additional connectors
- Sparkplug encoding and MQTT communication are implemented only once
- Gateway logic remains independent from individual source devices
- Configuration and monitoring can be centralized
- The architecture scales to heterogeneous industrial equipment
- Source devices remain simpler and require less protocol-specific logic

### Trade-offs

- The gateway becomes a central point of failure
- The gateway must handle data from multiple source devices
- Connector implementations are required for each supported source protocol
- Network communication between source devices and the gateway must be
  reliable

The benefits of centralized protocol handling and consistent data processing
outweigh the additional responsibility placed on the gateway.

---

## Result

The Industrial Edge Gateway provides a single integration point between
heterogeneous source devices and the MQTT/Sparkplug infrastructure.

New source types can be integrated by adding dedicated connectors without
modifying the core gateway processing pipeline.