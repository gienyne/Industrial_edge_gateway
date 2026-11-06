# Sprint 2

## Objective

Extend the connector-based architecture with a second source using a different communication protocol and validate the simultaneous operation of multiple connectors.

The selected source is **AquaControl**, an existing CODESYS irrigation application exposing process variables through its embedded OPC UA server.

The ESP32 communicates through MQTT/JSON, while AquaControl uses OPC UA. Both sources are converted into the common internal data model (`Metric`, `DeviceData`) and processed by the same Sparkplug B pipeline.

---

## Sprint 2 Architecture

```text
                    SOURCES
┌──────────────────────┐   ┌─────────────────────────┐
│     ESP32 Boards     │   │      AquaControl       │
│      MQTT / JSON     │   │   OPC UA / CODESYS      │
└──────────┬───────────┘   └────────────┬────────────┘
           │                            │
           ▼                            ▼
┌────────────────────────────────────────────────────┐
│              INDUSTRIAL EDGE GATEWAY               │
│                                                    │
│    ESP32Connector          OpcUaConnector           │
│           └────────────┬───────────┘                │
│                        ▼                           │
│                DeviceData / Metric                 │
│                        ▼                           │
│                 SparkplugEncoder                   │
│                        ▼                           │
│                  MQTTPublisher                     │
└────────────────────────┬───────────────────────────┘
                         │
                         ▼
                    MQTT Broker
```

One connector per protocol handles all configured sources of that type. Adding another OPC UA machine therefore requires a configuration entry, not a new connector implementation.

---

## Planned Tasks

### OPC UA Integration

- [x] Select AquaControl as the second source
- [x] Integrate `open62541pp` through vcpkg
- [x] Test a secure OPC UA connection independently
- [x] Generate a client certificate with the matching Application URI
- [x] Establish a `Basic256Sha256` / `SignAndEncrypt` session with user authentication
- [x] Implement `OpcUaConnector : IConnector`
- [x] Read five AquaControl variables: `tankLevel`, `pumpActive`, `valveActive`, `waterConsumption` and `rainSimActive`

### Gateway

- [x] Implement central configuration loading with `ConfigLoader`
- [x] Move credentials out of the source code
- [x] Run ESP32 and OPC UA connectors simultaneously
- [x] Add exception handling and reconnection with a 5-second cooldown
- [x] Assign measurement timestamps using the gateway clock
- [x] Preserve ESP32 uptime separately as `uptimeMs`
- [x] Reset `seq` at each NBIRTH and merge incoming metrics by name

### Packaging and Validation

- [x] Make the CMake build portable across Windows and Linux
- [x] Create a two-stage Docker build
- [x] Run the gateway in a Linux container
- [x] Validate NBIRTH, DBIRTH, continuous DDATA and NDEATH
- [x] Test OPC UA server outage and recovery
- [x] Test MQTT broker disconnection and reconnection

---

## Decisions

- **AquaControl instead of a simulation server:** an existing CODESYS project provides a real OPC UA server with meaningful variables and security requirements.
- **Polling instead of subscriptions:** the five slowly changing variables are read during each collection cycle. Subscriptions would add unnecessary complexity for this use case.
- **One endpoint per device:** each configured OPC UA endpoint represents one machine.
- **Gateway timestamps:** timestamps are assigned when data is collected. OPC UA `SourceTimestamp` is not used in this version.
- **Failure handling:** connectors retry independently. An unavailable source is handled through the gateway's 15-second device timeout, which triggers the corresponding device state transition.

---

## Problems Solved

| Problem | Cause | Solution |
|---|---|---|
| Session refused by the server | Application URI of the client did not match the certificate | Certificate with a matching SubjectAltName, client URI set through the native config handle |
| Value read failed (`Variant does not contain a value convertible…`) | CODESYS `REAL` is a 32-bit float | Convert `float`, `double` and `int32` to `Double` |
| `STATUS_HEAP_CORRUPTION` | Certificate buffers were destroyed while the client still referenced them | Keep certificate, key and client together in the per-source state |
| Crash when the OPC UA server stopped | Unprotected `runIterate()`; no exception handling in `main` | `try/catch` around the call, reconnection with cooldown, safety net in `main` |
| Phantom ESP32 devices after start | Retained MQTT messages from earlier tests | Clear the retained messages; later the connector ignores retained messages |

---

## Sprint 2 Success Criteria

- [x] Read live AquaControl values through a secured OPC UA session
- [x] Operate ESP32 and OPC UA connectors simultaneously
- [x] Publish both sources through the same Sparkplug B pipeline
- [x] Handle OPC UA server and MQTT broker outages
- [x] Keep credentials out of the source code
- [x] Run the gateway inside a Linux container

The Docker image was reduced from approximately **4.8 GB to 156 MB** using a two-stage build.

---

## Next Sprint

Add a backend ingestion service to consume the Sparkplug B stream and persist device metadata, measurements and events in a time-series database.

