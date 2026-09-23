# Industrial Edge Gateway Architecture

## Overview

This directory documents the architecture of the Industrial Edge Gateway.

The gateway follows a modular architecture based on separation of
responsibilities, dependency injection and programming to interfaces.

Source devices and industrial systems acquire their own data through
whichever means fit their protocol. The gateway receives that data through
dedicated connectors, converts it into a common internal representation, and
publishes the result as standardized Sparkplug B messages over MQTT.

This separation lets different source devices and industrial interfaces be
integrated without coupling the gateway core to a specific protocol.

---

## Architectural Boundary

The architecture is divided into two independent areas: source devices, and
the gateway. Each acquisition path stays independent end to end — they only
meet inside the gateway, once both have produced a `Metric`.

```text
         SOURCE DEVICES                              GATEWAY

  ESP32 / sensors                            ESP32Connector
  (DHT11, shock, light, button)              (subscribes to raw/+)
        │                                            ▲
        │ SensorConnector                            │ MQTT / JSON
        ▼                                            │
  raw/<deviceId>  ───────────────────────────────────┘
  (MQTT / JSON)


  Industrial machine                         OpcUaConnector
  (e.g. AquaControl / CODESYS)   ───────────► (direct OPC UA session,
                                                SignAndEncrypt)


                                                     │
                                             both converge as
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
                                               Mqttpublisher
                                                     │
                                                     ▼
                                                MQTT Broker
```

The ESP32 path and the OPC UA path do not share a transport: one is MQTT/JSON
on `raw/<deviceId>`, the other a direct OPC UA session. The gateway does not
require either to look like the other — both are simply expected to produce a
`Metric` on their own side of the boundary. See `gateway_connectors.md` for
the detail of each connector.

---

## Source Devices

A source device acquires data from its own hardware or protocol and is
responsible for nothing beyond that.

For the ESP32, a hardware abstraction layer sits before the raw transport:

```text
ESP32
 ├── DHT11Sensor
 ├── ShockSensor
 ├── LightSensor
 └── ButtonSensor      (not every board carries every sensor)
```

A source device never creates a `Metric` or a `DeviceData`, and never encodes
a Sparkplug message. The ESP32-specific layers (`ISensor`, `SensorConnector`)
are documented in `sensor_layer.md` and `sensor_connector.md`.

Not every source needs this layer: AquaControl (OPC UA) exposes its process
variables directly, with no sensor abstraction on the gateway's side of the
boundary — `OpcUaConnector` reads a node value and turns it directly into a
`Metric`.

---

## Gateway Connectors

The gateway integrates different sources through connectors implementing a
single interface, `IConnector`. Each connector represents one **protocol**,
not one machine — one instance can serve several devices.

```text
Source-specific data
        │
        ▼
     Connector            (implements IConnector)
        │
        ▼
       Metric
        │
        ▼
    DeviceData
```

Currently implemented: `ESP32Connector` (MQTT/JSON) and `OpcUaConnector`
(OPC UA). The gateway core does not need to know which one produced a given
`DeviceData`. Whether that `DeviceData` changes anything visible (a DDATA) or
nothing at all (unchanged metrics under Report-By-Exception) is decided by
`Gatewayapplication`, not by the connector — a connector always reports the
current state, never a delta. See `gateway_connectors.md` for the interface,
both implementations, and how failures are handled.

---

## Common Internal Data Model

`Metric` and `DeviceData` are the contract between source integration and
gateway processing; everything downstream of them is source-independent.

```text
Metric
   │
   ▼
DeviceData
```

`Metric` is one standardized measurement. `DeviceData` groups the metrics of
one physical device. Each connector produces its own `DeviceData` objects;
different devices are never merged. See `data_models.md` for every structure,
who owns each one, and how a value's representation changes as it crosses
the pipeline.

---

## Sparkplug and MQTT

Sparkplug B is produced centrally by the gateway; no source implements it.

`SparkplugEncoder` owns the Sparkplug representation, including topics,
Protobuf payloads, `seq` and `bdSeq`. `Mqttpublisher` owns the MQTT session
and transport.

See `sparkplug_encoder.md` and `mqtt_publisher.md` for the details of each.

---

## Configuration

All gateway and connector settings live in one JSON file, loaded once at
startup by `ConfigLoader` and parsed into one typed structure per owning
component — there is no single global configuration object.

```text
config/config.json
        │
        ▼
   ConfigLoader
        │
   ┌────┼──────────────────────┐
   ▼    ▼                      ▼
GatewayApplicationConfig  ESP32ConnectorConfig  OpcUaConnectorConfig
```

Gateway configuration and source-device configuration (the ESP32's own
`Config.h`) are independent; changing one never requires touching the other.
See `configuration.md`.

---

## Application Composition

`main.cpp` is the composition root: it loads the configuration, creates the
connectors, and builds `Gatewayapplication`. `Gatewayapplication` itself is
the runtime orchestrator — it owns the Sparkplug pipeline and drives the
Sparkplug lifecycle (birth, data, rebirth, death), but it does not construct
itself or decide what exists in the object graph.

```text
main.cpp                          Gatewayapplication
(composition root)                (runtime orchestrator)
     │                                    │
     ├── ConfigLoader                     ├── connectors_     (IConnector)
     ├── ESP32Connector      ────inject───┤
     ├── OpcUaConnector      ────inject───┤
     │                                    ├── encoder_        (SparkplugEncoder)
     └── Gatewayapplication  ─────────────┴── publisher_      (Mqttpublisher)
```

Adding a new connector means one addition in `main.cpp` and one section in the
configuration file; `Gatewayapplication` itself does not change. See
`gateway_application.md` for the full lifecycle: discovery, birth, rebirth,
Report-By-Exception and device timeout.

---

## Design Principles

* **Single Responsibility.** Each component has one clearly defined job.
* **Dependency Injection.** Components receive what they need; nothing is looked up globally.
* **Programming to Interfaces.** `IConnector` provides the common abstraction for source integrations.
* **Common Internal Data Model.** Source-specific data becomes `Metric` and `DeviceData` before it reaches the central pipeline.
* **Hardware and Protocol Independence.** The core knows neither sensor hardware nor MQTT/Sparkplug/OPC UA specifics outside the components whose job that is.
* **Extensibility.** A new connector needs to produce `Metric` and `DeviceData`; the rest of the pipeline is unchanged.

---

## Documentation Structure

| Document                 | Description                                      |
| ------------------------ | ------------------------------------------------ |
| `overview.md`            | This document                                    |
| `data_models.md`         | The common internal data models                  |
| `configuration.md`       | Gateway and connector configuration              |
| `sensor_layer.md`        | ESP32 hardware abstraction (`ISensor`)           |
| `sensor_connector.md`    | ESP32 sensor aggregation (`SensorConnector`)     |
| `gateway_connectors.md`  | `IConnector`, `ESP32Connector`, `OpcUaConnector` |
| `sparkplug_encoder.md`   | Sparkplug B encoding                             |
| `mqtt_publisher.md`      | MQTT session and transport                       |
| `gateway_application.md` | Composition root, orchestration, lifecycle       |
| `adr/`                   | Architecture Decision Records                    |

---

## Recommended Reading Order

1. `overview.md`
2. `data_models.md`
3. `sensor_layer.md`
4. `sensor_connector.md`
5. `gateway_connectors.md`
6. `sparkplug_encoder.md`
7. `mqtt_publisher.md`
8. `gateway_application.md`
9. `configuration.md`
10. `adr/`

---

## Architectural Evolution

The architecture is designed to evolve by adding source connectors and
gateway components rather than modifying the central processing pipeline.

Candidates for the future include a `ModbusConnector`, a `RESTConnector`, a
database ingestion service, a dashboard, and health monitoring of the
gateway itself. None of these require a change to `Metric`, `DeviceData`, or
the Sparkplug encoding and MQTT transport layers — the common internal data
model is the stable contract that makes that possible.
