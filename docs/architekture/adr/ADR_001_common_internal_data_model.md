# ADR-001: Common Internal Data Model

## Status

Accepted

---

## Context

The Industrial Edge Gateway receives data from different types of source
devices and communication interfaces.

These sources expose different hardware interfaces, communication mechanisms
and native data representations.

Without a common internal representation, source-specific data would propagate
into downstream gateway components, creating tight coupling between data
acquisition, application logic and communication protocols.

Sparkplug B also requires measurements to be associated with a specific device.

---

## Decision

The Industrial Edge Gateway shall use a common internal data model as the
stable contract between data acquisition and gateway processing.

The gateway-wide internal representation consists of `Metric` and
`DeviceData`.

`SensorReading` is not part of the mandatory gateway-wide model. It is an
optional source-specific intermediate representation used when required by a
source implementation.

Connectors are responsible for converting source-specific data into the
common gateway representation before it enters the gateway processing
pipeline.

See `data-models.md` for the detailed model definitions and `overview.md`
for the architectural boundaries.

---

## Consequences

### Advantages

- Strong separation between source devices and gateway logic
- New connectors can be added without modifying the gateway core
- Sparkplug encoding remains independent of the data source
- MQTT publishing remains independent of sensors and industrial protocols
- Source-specific acquisition models remain isolated from the gateway core
- Improved testability through protocol-independent data structures
- Clear ownership of data between source devices and the gateway
- Simplified future integration of industrial machines

### Trade-offs

- Every connector must convert its native data into the gateway's internal
  representation
- Some sources require an additional source-specific representation before
  conversion into `Metric`
- The abstraction introduces a small amount of conversion code

The architectural flexibility and separation of responsibilities gained from
the common internal data model outweigh this additional complexity.

---

## Result

The common internal data model provides a stable boundary between source
devices and gateway processing.

Source-specific implementations can evolve independently while the gateway
core continues to operate on standardized `Metric` and `DeviceData`
representations.
