# ADR-003: Connector-Based Integration

## Status

Accepted

---

## Context

The Industrial Edge Gateway must integrate data from different types of
source systems, including ESP32 devices and future industrial protocols such
as OPC UA and Modbus.

Each source technology has different communication mechanisms and acquisition
logic.

If `GatewayApplication` depended directly on every concrete connector type,
adding a new source would require modifications to the gateway core.

The gateway therefore needs a stable integration boundary between source
connectors and the application core.

---

## Decision

The gateway shall integrate source systems through a common `IConnector`
interface.

Each source type is implemented by a concrete connector, while
`GatewayApplication` interacts with connectors through the common interface.

The interface and its concrete implementations are documented in
`gateway-connectors.md`.

The architectural consequence can be illustrated as follows:

```text
Without IConnector
────────────────────────────────

GatewayApplication
        │
        ├── ESP32Connector
        ├── OPCUAConnector
        ├── ModbusConnector
        └── RESTConnector


With IConnector
────────────────────────────────

GatewayApplication
        │
        ▼
    IConnector
     /   |   \
    ▼    ▼    ▼
  ESP32 OPCUA Modbus
```

With this structure, the gateway core depends on the connector abstraction
rather than on the concrete source technologies.

---

## Consequences

### Advantages

- New source types can be integrated through additional connector
  implementations.
- `GatewayApplication` remains independent from source-specific acquisition
  logic.
- Existing gateway processing components do not need to change when a new
  connector is introduced.
- Different source technologies can coexist within the same gateway.
- Connector implementations can be tested independently from the gateway
  application.
- The common `DeviceData` model remains the stable boundary between source
  integration and the gateway core.

### Trade-offs

- An additional abstraction layer is introduced.
- Each new source type requires a corresponding connector implementation.
- The gateway must manage the lifecycle of multiple connector instances.

The additional abstraction is considered justified because it prevents
source-specific dependencies from spreading into the gateway core.

---

## Result

The gateway uses `IConnector` as the stable integration boundary for external
source systems.

Concrete connector implementations remain responsible for source-specific
communication and data acquisition, while the gateway core operates on the
common connector interface and internal data model.