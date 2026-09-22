# Gateway Application

## Purpose

`Gatewayapplication` is the orchestrator of the Industrial Edge Gateway.

It receives its connectors from `main.cpp`, owns the Sparkplug pipeline
(`SparkplugEncoder` and `Mqttpublisher`) and drives the Sparkplug lifecycle
of the Edge Node and of every device the gateway discovers: birth, data,
rebirth and death.

It contains the gateway's decisions (what is published, and when) but none of
the mechanics of acquiring, encoding or transporting data.

---

## Responsibilities

The application

- initializes the connectors;
- discovers the initial set of devices during a startup discovery window;
- publishes the Sparkplug birth sequence (NBIRTH, then one DBIRTH per device);
- detects new devices and new metrics and republishes the birth sequence;
- applies Report-By-Exception (RBE): only changed metrics are sent as DDATA;
- detects silent devices and publishes DDEATH;
- handles a Node Rebirth command (NCMD) requested by a Host Application;
- shuts down gracefully (NDEATH, then MQTT disconnect).

The application never

- communicates with a source device or an industrial protocol;
- builds Sparkplug messages;
- talks to the MQTT broker directly.

---

## Position in the Architecture

```text
                              main.cpp
                                 │
                  ConfigLoader + object creation
                                 │
                                 ▼
                       ┌───────────────────┐
                       │ Gatewayapplication│
                       │                   │
                       │  Orchestration    │
                       └─────────┬─────────┘
                                 │
            ┌────────────────────┼────────────────────┐
            │                    │                    │
            ▼                    ▼                    ▼
      ┌─────────────┐    ┌─────────────── ┐    ┌──────────────┐
      │ connectors_ │    │   encoder_     │    │  publisher_  │
      │ IConnector  │    │SparkplugEncoder│    │ Mqttpublisher │
      └──────┬──────┘    └───────┬─────── ┘    └───────┬──────┘
             │                   │                    │
             │ DeviceData        │ SparkplugPayload   │ MQTT
             └──────────────►────┴──────────────►─────┘
                                                      │
                                                      ▼
                                                MQTT Broker
```

The application does not know how a machine is accessed. It only sees
`DeviceData` coming out of connectors and `SparkplugPayload` going into the
publisher.

---

## Composition Root: `main.cpp`

`main.cpp` builds the object graph; `Gatewayapplication` runs it.

```text
                    config/config.json
                            │
                            ▼
                    config::loadFile()
                            │
          ┌─────────────────┼─────────────────┐
          ▼                 ▼                 ▼
 parseEsp32Config()   parseOpcUaConfig()   parseGatewayConfig()
          │                 │                 │
          ▼                 ▼                 ▼
   ESP32Connector     OpcUaConnector    GatewayApplicationConfig
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
                            ▼
                   Gatewayapplication
```

A configuration error aborts the startup with a message and exit code 1.
Adding a new source means adding one connector in `main.cpp` and one section
in the configuration file; the application itself does not change.

The run loop lives in `main.cpp`:

```text
                  ┌──────────────────────┐
                  │      initialize()    │
                  └──────────┬───────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ running == true │◄──────────────┐
                    └────────┬────────┘               │
                             │ yes                    │
                             ▼                        │
                        pollOnce()                    │
                             │                        │
                             ▼                        │
                       sleep 500 ms                   │
                             │                        │
                             └────────────────────────┘
                             │
                       running == false
                             │
                             ▼
                    ┌─────────────────┐
                    │    shutdown()   │
                    └─────────────────┘
```

`SIGINT` (Ctrl+C) and `SIGTERM` (sent by a container runtime or service
manager) share the same handler, which only clears the `running` flag. The
graceful shutdown itself runs on the main thread, outside the signal handler.

---

## Public Interface

```cpp
struct GatewayApplicationConfig
{
    SparkplugEncodeConfig encoderConfig;   // namespace, group, edge node id
    MQTTPublisherConfig   mqttConfig;      // broker, client id, bdSeq file
    std::chrono::milliseconds discoveryWindow{3000};
    std::chrono::milliseconds deviceTimeout{15000};
};

class Gatewayapplication
{
public:
    Gatewayapplication(const GatewayApplicationConfig& config, std::vector<std::unique_ptr<IConnector>> connectors);

    bool initialize();
    void pollOnce();
    void shutdown();
};
```

`initialize()` returns `false` if the initial MQTT connection fails or if the
initial birth sequence cannot be published; `main.cpp` then exits with code 1.
A connector that fails to initialize does not cause this (see below).

---

## Internal State

| Member            | Role                                                                 |
|-------------------|----------------------------------------------------------------------|
| `config_`         | Gateway settings                                                     |
| `encoder_`        | `SparkplugEncoder`, built from `encoderConfig`                       |
| `publisher_`      | `Mqttpublisher`, built from `mqttConfig` and a reference to `encoder_` |
| `connectors_`     | Injected connectors                                                  |
| `birthedDevices_` | Devices for which a DBIRTH was published; detects new devices       |
| `lastKnownState_` | Last known value of every metric per device, merged over time; basis of RBE and of DBIRTH content |
| `lastSeenAt_`     | Last time data arrived per device; basis of timeout detection        |

Members are initialized in declaration order. `encoder_` is declared before
`publisher_` because the publisher holds a reference to the encoder.

The connectors are used through the `IConnector` interface. The encoder and
the publisher are currently held as concrete types; see *Known Limitations*.

---

## Initialization


```text
initialize()
    │
    ├──► Initialize all connectors
    │          │
    │          └── failure -> log and continue
    │
    ├──► Startup discovery window
    │          │
    │          └── 3 s / poll every 200 ms
    │
    ├──► Wait until birth publication is allowed
    │          │
    │          └── isAllowedToPublishBirth()
    │
    ├──► Connect to MQTT broker
    │          │
    │          └── register NDEATH as Last Will
    │
    └──► Publish birth sequence
               │
               ├── NBIRTH
               └── DBIRTH for every discovered device
```

**Connector failures are not fatal.** A connector that cannot reach its source
at startup (CODESYS not running yet, ESP32 not powered) stays registered and
retries with a cooldown from inside `collectData()`. The gateway starts anyway
and the device appears later through a rebirth.

**The discovery window** groups the devices that are already present at
startup into a single birth sequence. Without it, every device discovered one
after the other would trigger its own rebirth of the whole node.

**Birth permission.** After the discovery window the application waits until
`isAllowedToPublishBirth()` (`BirthPublicationPolicy.h`) returns `true`, and
only then opens the MQTT session. It always returns `true` in the current
version because no Primary Host Application is configured. It is the single
place to extend if Host `STATE` handling is added.

**An MQTT connection failure at startup is fatal**, unlike a connector failure:
without a broker the gateway has nowhere to publish, so `initialize()` returns
`false`. Only a session lost *after* startup is recovered automatically.

---

## Poll Cycle

```text
                         pollOnce()
                             │
                             ▼
                  ┌─────────────────────┐
                  │ Rebirth requested?  │
                  └──────────┬──────────┘
                         yes │
                             ▼
                      Publish births
                             │
                             └──────► end
                             
                         no  │
                             ▼
             Collect data from all connectors
                             │
                             ▼
                  Refresh lastSeenAt_
                             │
                             ▼
                New device / new metric?
                       │              │
                     yes             no
                       │              │
                       ▼              ▼
                 Update state    Filter changed
                       │             metrics
                       ▼              │
                 Publish births       ▼
                       │           DDATA
                       │              │
                       └──────┐───────┘
                              ▼
                    Check device timeouts
                              │
                              ▼
                            DDEATH
```

A cycle that ends with a rebirth publishes no DDATA and skips the timeout
check; the new DBIRTH already carries the current values.

### Rebirth

A rebirth republishes NBIRTH followed by the DBIRTH of every known device.
A *logical* rebirth (new device, NCMD) reuses the current `bdSeq`, because the
MQTT session, and therefore its Last Will, has not changed. The NBIRTH restarts
`seq` at 0, exactly as for a new session.

| Trigger                           | Detection                                          |
|-----------------------------------|----------------------------------------------------|
| A new device appears              | its id is not in `birthedDevices_`                 |
| A known device reports a new metric | `hasNewMetric(previous, incoming)`               |
| A Host Application requests it    | NCMD `Node Control/Rebirth = true`                 |
| A device comes back after a timeout | it was erased at DDEATH, so it is new again      |

A successful reconnection of the MQTT session also requests a rebirth:
`Mqttpublisher` raises the same flag as for an NCMD. Because this is a *new*
session, it comes with a new `bdSeq` and `seq` restarts at 0.

The state is updated *before* the births are published, so the DBIRTH already
contains the new structure. A metric that was never declared in a DBIRTH must
never appear in a DDATA; this is why a new metric triggers a rebirth instead of
being published directly.

Republishing every DBIRTH, not only the new device's, is a deliberate choice:
every Host, present or future, keeps the same complete view of the topology,
and the same mechanism serves both discovery and the mandatory NCMD rebirth.

### Report-By-Exception

`filterChangedMetrics(previous, incoming)` returns a `DeviceData` containing
only the metrics whose value differs from `lastKnownState_`. If nothing
changed, nothing is published. New metrics never reach this step, because they
trigger a rebirth first. The known state is merged by metric name and advances
only after the DDATA was published successfully, so a failed publication is
retried on the next cycle.

A metric that goes missing while the device keeps sending other data does not
cause a rebirth when it returns: it stays declared with its last value, and a
DDATA is sent only if the new value differs from that last value.

### Device timeout

Because of RBE, silence on the broker does not mean the device is gone.
The application therefore tracks `lastSeenAt_`, refreshed whenever a connector
returns data for the device (even if no metric changed). When a device has
been silent for longer than `deviceTimeout` (15 s by default) the application
publishes DDEATH and erases the device's state, even if the DDEATH publication
failed. Only devices already declared by a DBIRTH are checked.

Sparkplug requires a DDEATH when a device is lost but does not define how the
loss is detected. The timeout is therefore an implementation decision. It must
stay above the longest normal silence of a device: an ESP32 reports every 2 s,
connected OPC UA sources are read on every cycle, and a source that is down is retried once per cooldown.

A `DeviceData` with no metrics (a connector received something it could not
decode) refreshes `lastSeenAt_` only. It proves the transport is alive without
publishing anything.

---

## Sparkplug Lifecycle

| Message | Published when                                                        |
|---------|-----------------------------------------------------------------------|
| NBIRTH  | after the MQTT connection, and on every rebirth                       |
| DBIRTH  | right after NBIRTH, one per known device                              |
| DDATA   | a known device has changed metrics                                    |
| DDEATH  | a device exceeded `deviceTimeout`                                     |
| NDEATH  | graceful shutdown (published by the application) or unexpected loss (published by the broker as the Last Will) |

---

## Shutdown

```text
shutdown()
   │
   ├─ publish NDEATH explicitly
   └─ disconnect from the MQTT broker
```

With MQTT 3.1.1 a clean DISCONNECT makes the broker discard the Last Will, so
the application publishes NDEATH itself before disconnecting. NDEATH already
implies that every device of the node is gone; no per-device DDEATH is sent.

If the process dies unexpectedly, the broker publishes the Last Will registered
at connection time. The `bdSeq` of that NDEATH matches the one in the NBIRTH of
the same session.

---

## Failure Isolation

- Connector failures stay inside the connectors: a failing source is retried
  after a cooldown and simply produces no data in the meantime. The connection
  attempts themselves can delay the cycle (see *Known Limitations*).
- The application turns that silence into DDEATH through the device timeout.
- An exception escaping `pollOnce()` is caught in `main.cpp`; the gateway
  keeps running.

---

## Design Principles

- Orchestration without implementation: acquisition, encoding and transport
  are delegated.
- Dependency injection for connectors.
- Decisions kept in one place: birth, rebirth, RBE and timeout logic all live
  in this class.
- Explicit lifecycle: initialize, poll, shutdown.
- Failures degrade to silence, and silence is handled uniformly.

---

## Known Limitations

- `Gatewayapplication` holds `SparkplugEncoder` and `Mqttpublisher` as concrete
  types. `IsparkplugEncoder` exists but is not used at this level, and an
  `IMqttPublisher` interface was never introduced, so the publisher cannot be
  replaced by a mock in unit tests yet.
- The polling period (500 ms) is fixed in `main.cpp`.
- `deviceTimeout` is global; per-source timeouts are not supported.
- **No per-metric freshness.** `lastKnownState_` is merged by metric name: a
  metric missing from one payload (a failed DHT11 read, an unreadable OPC UA
  node) keeps its last value and its original timestamp, and its return does not
  cause a rebirth. Nothing tells a Host that such a value has become stale. If
  that is needed later, Sparkplug's `is_null` flag is the natural way to say
  "no current value".
- **A slow connector stalls the whole cycle.** Connectors are called one after
  the other on the main thread. An OPC UA source that is down blocks
  `collectData()` for the duration of the connection attempt (about 2 s against
  a closed local port, once per cooldown), which delays every other device, every
  DDATA and the reaction to an NCMD Rebirth.
- **A DDATA can slip out between an NCMD and the NBIRTH.** The rebirth flag is
  only read at the start of a cycle. If an NCMD arrives while a cycle is already
  running (typically one stuck in a slow connector) and that cycle still has data
  to publish, its DDATA goes out before the next cycle publishes the NBIRTH. The
  message still belongs to the previous sequence: its `seq` continues it, and its
  metrics were declared in the previous DBIRTH. The pending rebirth is processed
  at the start of the next cycle.

---

## Future Extensions

- Database ingestion service and dashboard, consuming the Sparkplug stream.
- Health monitoring of the gateway itself.
- Primary Host Application `STATE` handling, plugged into
  `isAllowedToPublishBirth()`.
- Additional connectors (Modbus, REST) require no change in this class.
