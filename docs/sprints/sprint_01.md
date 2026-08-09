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
- [ ] Configure PlatformIO
- [ ] Connect KY-015
- [ ] Connect KY-002
- [ ] Connect KY-018
- [ ] Read sensor values
- [ ] Implement source-side sensor model
- [ ] Implement `SensorConnector`
- [ ] Implement MQTT communication
- [ ] Publish sensor data through MQTT
- [ ] Verify MQTT messages from the ESP32

### Industrial Edge Gateway

- [ ] Set up Gateway build environment
- [ ] Implement `Metric`
- [ ] Implement `DeviceData`
- [ ] Implement `IConnector`
- [ ] Implement `ESP32Connector`
- [ ] Receive ESP32 data through MQTT
- [ ] Convert source data into `DeviceData`
- [ ] Implement `SparkplugEncoder`
- [ ] Generate Sparkplug B NBIRTH
- [ ] Generate Sparkplug B DDATA
- [ ] Implement `MQTTPublisher`
- [ ] Publish Sparkplug B messages through MQTT
- [ ] Verify Sparkplug messages with MQTT Explorer

### Integration

- [ ] Validate ESP32 -> MQTT Broker -> Gateway data flow
- [ ] Validate Gateway -> Sparkplug B -> MQTT Broker data flow
- [ ] Verify device identity
- [ ] Verify metric values
- [ ] Verify Sparkplug topic structure
- [ ] Document results

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

## Progress

| Date | Description |
|------|--------------|
| ...  | ...          |

## Notes

...

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
