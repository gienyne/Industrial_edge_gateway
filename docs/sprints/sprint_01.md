# Sprint 1

## Objective

Build and validate the first end-to-end data flow between an ESP32 source
device and the Industrial Edge Gateway.

The ESP32 acquires data from three sensors and publishes the source data
through MQTT.

The Industrial Edge Gateway receives the data through the `ESP32Connector`,
converts it into the common internal data model, encodes it as Sparkplug B
and publishes the resulting messages through MQTT.

The ESP32 does not implement Sparkplug B.

---

## Sprint 1 Architecture

The Sprint 1 implementation uses one source device:

```text
                    SOURCE DEVICE
┌───────────────────────────────────────────┐
│                   ESP32                   │
│                                           │
│   KY-015      KY-002       KY-018         │
│      │           │           │            │
│      └───────────┼───────────┘            │
│                  ▼                        │
│           SensorConnector                 │
└──────────────────┬────────────────────────┘
                   │
                   │ MQTT
                   ▼
             MQTT Broker
                   │
                   │ MQTT
                   ▼
┌───────────────────────────────────────────┐
│          INDUSTRIAL EDGE GATEWAY          │
│                                           │
│            ESP32Connector                 │
│                  │                        │
│                  ▼                        │
│              DeviceData                   │
│                  │                        │
│                  ▼                        │
│           SparkplugEncoder                │
│                  │                        │
│                  ▼                        │
│             MQTTPublisher                 │
└──────────────────┬────────────────────────┘
                   │
                   │ Sparkplug B / MQTT
                   ▼
              MQTT Broker
```

Sparkplug B is introduced only at the Gateway level. The source device
remains independent of the Sparkplug representation.

---

## Planned Tasks

### ESP32 Source Device

- [x] Create Git repository
- [x] Configure PlatformIO
- [x] Connect KY-015
- [x] Connect KY-002
- [x] Connect KY-018
- [x] Read sensor values
- [x] Implement source-side sensor model
- [x] Implement `SensorConnector`
- [x] Implement MQTT communication
- [x] Publish sensor data through MQTT
- [x] Verify MQTT messages from the ESP32

### Industrial Edge Gateway

- [x] Set up Gateway build environment
- [x] Implement `Metric`
- [x] Implement `DeviceData`
- [x] Implement `IConnector`
- [x] Implement `ESP32Connector`
- [x] Receive ESP32 data through MQTT
- [x] Convert source data into `DeviceData`
- [x] Implement `SparkplugEncoder`
- [x] Generate Sparkplug B NBIRTH
- [x] Generate Sparkplug B DDATA
- [x] Implement `MQTTPublisher`
- [x] Publish Sparkplug B messages through MQTT
- [x] Verify Sparkplug messages with MQTT Explorer

### Integration

- [x] Validate ESP32 -> MQTT Broker -> Gateway data flow
- [x] Validate Gateway -> Sparkplug B -> MQTT Broker data flow
- [x] Verify device identity
- [x] Verify metric values
- [x] Verify Sparkplug topic structure
- [x] Document results

---

## MQTT Communication

The same MQTT broker is used for both flows, kept logically separate
through distinct topic namespaces (source-side vs. Sparkplug).

The source-side MQTT topic structure is implementation-specific to Sprint 1
and may evolve with the connector implementation.

The ESP32 does not generate Sparkplug B messages; Sparkplug encoding is
performed exclusively by the Gateway.

---

## Sparkplug Scope

Sparkplug B is implemented exclusively in the Industrial Edge Gateway.

The Sprint 1 Gateway is responsible for generating at least:

- NBIRTH
- DDATA

The Sparkplug lifecycle and additional message types will be extended as
required by later implementation stages.

---

## Decisions

### 2026-08-03

- ESP32 selected as the first prototype source device.
- KY-015, KY-002 and KY-018 selected as the Sprint 1 sensors.
- ESP32 communicates with the Gateway through MQTT.
- The ESP32 does not implement Sparkplug B.
- Sparkplug B encoding is performed exclusively by the Industrial Edge
  Gateway.
- A public MQTT broker is used during Sprint 1 for initial communication
  testing.
- Public broker topics must not expose laboratory-specific names or
  identifying information.
- The public broker is used only for prototype validation.
- The laboratory MQTT broker will be used when integrating real laboratory
  machines.
- No database is required for Sprint 1.
- No dashboard is required for Sprint 1.
- The source-to-gateway transport remains an implementation detail of
  `ESP32Connector`.

---


## Sprint 1 Success Criteria

Sprint 1 is considered successful when:

- The ESP32 successfully acquires data from KY-015, KY-002 and KY-018.
- The ESP32 publishes the acquired data through MQTT.
- `ESP32Connector` receives and interprets the source data.
- The Gateway produces valid `DeviceData`.
- `SparkplugEncoder` generates valid Sparkplug B messages.
- The Gateway publishes NBIRTH and DDATA.
- The complete data flow can be observed and verified with MQTT Explorer.

---

## Next Sprint

Extend the Gateway and source integration based on the results of Sprint 1.
