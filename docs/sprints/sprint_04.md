# Sprint 4

## Objective

Make the persisted data accessible through a secured REST API and a web dashboard.

The backend manages the lifecycle and status of Edge Nodes and devices, stores events and exposes measurements, history and device information. A React dashboard provides access to this data through user authentication.

---

## Sprint 4 Architecture

```text
Gateway ─ Sparkplug B / MQTT ──► MQTT Broker
                                      │
                                      ▼
                         ┌─────────────────────────┐
                         │         Backend         │
                         │                         │
                         │   Lifecycle & Events    │
                         │       Ingestion         │
                         │       REST API          │
                         │    Session + CSRF       │
                         └───────────┬─────────────┘
                                     │
                            ┌────────┴────────┐
                            ▼                 ▼
                       TimescaleDB       React Dashboard
                                         REST / JSON
```

The backend handles Sparkplug messages, manages device states and events, persists the data and exposes it through the REST API. The dashboard communicates exclusively with the backend.

---

## Planned Tasks

### Backend: Lifecycle and Events

- [x] Implement Edge Node status management with `bdSeq`
- [x] Implement `ONLINE`, `OFFLINE` and `UNKNOWN` states for Edge Nodes and devices
- [x] Process `NBIRTH`, `NDEATH`, `DBIRTH`, `DDATA` and `DDEATH`
- [x] Persist lifecycle events and sequence-gap events
- [x] Request Node Rebirth at backend startup and after MQTT reconnection
- [x] Implement synchronization after broker reconnection
- [x] Detect sequence gaps and request Rebirth

### Backend: Security and REST API

- [x] Implement user authentication with BCrypt password hashes
- [x] Implement session-based authentication and CSRF protection
- [x] Create the initial user through environment variables
- [x] Implement password change
- [x] Provide endpoints for overview, devices and device details
- [x] Implement recent measurements and metric history
- [x] Implement event filtering and pagination
- [x] Add consistent JSON error responses and Swagger UI

### Dashboard

- [x] Set up React, TypeScript, Material UI and Vite
- [x] Implement login and session expiration handling
- [x] Create Overview, Devices and Device Details pages
- [x] Implement Metrics & History with predefined and custom time ranges
- [x] Implement Events with filters and event details
- [x] Add Settings for account, password and application information
- [x] Display synchronization status

### Tests and Documentation

- [x] Add message processor unit tests
- [x] Add REST controller tests with MockMvc and service tests
- [x] Document backend and dashboard

---

## Decisions

- **Custom dashboard instead of Grafana:** React provides control over the device status model, event presentation and historical state charts.
- **Polling instead of WebSocket:** the dashboard periodically retrieves updates from the REST API (2 s for status, 5 s for events and live charts). This keeps the communication model simple.
- **Status handling:** `UNKNOWN` means that the status has not yet been confirmed. It is not automatically changed to `OFFLINE` by a timeout; death messages determine the offline state.
- **Event semantics:** `DEVICE_ONLINE` is recorded only for a real transition to ONLINE, not when an UNKNOWN device is simply confirmed as online.
- **Offline history:** state charts stop at the device's last contact to avoid displaying stale states as new measurements.
- **Security:** the dashboard uses session cookies and CSRF protection.
---

## Sprint 4 Success Criteria

- [x] Correct Edge Node and device states after gateway, backend and broker restarts.
- [x] Display devices, current values, history and events from ESP32 and OPC UA sources.
- [x] Restrict data access to authenticated users.
- [x] Allow users to change their passwords.
- [x] Pass the automated backend tests.

---

## Next Sprint

Sprint 5 integrates real Smart Factory equipment into the Industrial Edge Gateway.
