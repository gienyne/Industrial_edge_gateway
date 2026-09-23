# MQTT Publisher

## Purpose

`Mqttpublisher` handles all MQTT communication between the gateway and the
broker, using the Eclipse Paho asynchronous C++ client.

It transports already-encoded Sparkplug messages. It does not create or
interpret outgoing Sparkplug messages. The only Sparkplug-specific input it
handles is the incoming Node Control/Rebirth command, because it owns the
NCMD subscription.

---

## Responsibilities

The publisher

* establishes the MQTT session and registers the Last Will;
* obtains the session's `bdSeq` and hands it to the encoder;
* publishes `SparkplugPayload` objects exactly as given (topic, bytes, QoS, retain);
* subscribes to the Node command topic (NCMD) and detects a Rebirth request;
* recovers a lost session, lazily and with a cooldown, and requests a rebirth when it succeeds;
* disconnects cleanly.

The publisher never

* reads sources or creates metrics or `DeviceData`;
* builds Sparkplug messages;
* decides when to publish or how to react to a rebirth request;
* stores data.

---

## Position in the Architecture

```text
                          Gateway
                             │
              ┌──────────────┴──────────────┐
              │                             │
              ▼                             ▼
     SparkplugEncoder                  Mqttpublisher
              │                             │
              │ SparkplugPayload            │ MQTT
              └────────────────────────────►│
                                            │
                                            ▼
                                      MQTT Broker
                                            │
                                            │ NCMD / Rebirth
                                            ▼
                                      Mqttpublisher
                                            │
                                            │ atomic flag
                                            ▼
                                    Gatewayapplication
```

---

## Public Interface

```cpp
struct MQTTPublisherConfig
{
    std::string brokerAddress;
    std::string clientId;
    std::string bdSeqFilePath = "bdseq.dat";
};

class Mqttpublisher : public virtual mqtt::callback
{
public:
    Mqttpublisher(const MQTTPublisherConfig& config, SparkplugEncoder& encoder);

    bool initialize();
    bool publish(const SparkplugPayload& payload);
    bool disconnect();

    bool consumeRebirthRequest();

    void message_arrived(mqtt::const_message_ptr msg) override;
    void connection_lost(const std::string& lst) override;

    bool connectSession();
};
```

The publisher receives the encoder by reference because a session needs two
things from it: the Last Will payload and the topic of the command
subscription.

---

## Session Lifecycle

Opening an MQTT session (at startup and after every reconnection) does the
following:

```text
BdSeqManager.nextSessionBdSeq()      new session number (persisted file)
        │
        ▼
encoder.setBdSeq(bdSeq)              Will and NBIRTH carry the same value; seq restarts at 0
        │
        ▼
encoder.buildWillPayload()           NDEATH, ready to register
        │
        ▼
CONNECT with Last Will               clean session; QoS and retain taken from the payload
        │
        ▼
BdSeqManager.commitSessionBdSeq()    value written back after a successful CONNECT
        │
        ▼
subscribe to NCMD topic (QoS 1)      encoder.nodeCommandTopic()
```

The Last Will must be registered before the connection is opened, which is why
the NDEATH payload is built first. If the gateway disappears without a clean
shutdown, the broker publishes it on its behalf. NBIRTH itself is published by
the application after `initialize()`, not by the publisher. A failure to open
the session at startup makes `initialize()` return `false`, and the gateway
does not start.

---

## Publishing

`publish()` applies `topic`, `payload`, `qos` and `retain` of the
`SparkplugPayload` unchanged. The QoS and retain values are decided by the
encoder, per message type, so the publisher holds no Sparkplug rules.

`publish()` is synchronous: it waits for the Paho delivery token before
returning `true`, and returns `false` on any MQTT error.

**Recovery is lazy and happens inside `publish()`.** `connection_lost` only logs.
When `publish()` finds the session down, it tries to reopen it, at most once per
cooldown period (5 s, `reconnectCooldown`); within the cooldown it just returns
`false`. If the reconnection succeeds, a **new Sparkplug session** has started
(new `bdSeq`, new Last Will), so the publisher raises the rebirth flag itself
and still returns `false` for the message that triggered the attempt, which is
dropped. On the next `pollOnce()` the application consumes the flag and
republishes NBIRTH and every DBIRTH, exactly as for an NCMD rebirth.

---

## Receiving Node Commands

The Paho callback thread and the gateway's main thread must not share
application state. The publisher therefore keeps the interaction minimal:

```text

        Paho callback thread                    Gateway main thread
                 │                                      │
                 │  NCMD: Rebirth = true                │
                 ▼                                      │
        message_arrived()                               │
                 │                                      │
                 ▼                                      │
      rebirthRequested_ = true                          │
                 │                                      │
                 │                                      │
                 │                         pollOnce()   │
                 │                                      ▼
                 │                         consumeRebirthRequest()
                 │                                      │
                 │                                      ▼
                 │                            rebirth requested
                 │                                      │
                 │                                      ▼
                 │                            publish NBIRTH
                 │                            publish DBIRTH

```

`consumeRebirthRequest()` returns `true` when a rebirth is pending and clears
the flag. An `std::atomic<bool>` is a state flag, not a queue: multiple requests
arriving before the flag is consumed collapse into a single pending rebirth.
The same flag is raised after a successful reconnection. What to do about it is
`Gatewayapplication`'s decision. No mutex is shared between the two threads.

An incoming payload that cannot be decoded is logged and ignored. The
subscription is QoS 1, while a Host publishes NCMD with QoS 0 as the
specification requires; the two values are independent.

---

## Shutdown

`disconnect()` closes the session cleanly. With MQTT 3.1.1 a clean DISCONNECT
makes the broker discard the Last Will, so the application publishes the NDEATH
itself just before calling it.

---

## Security

The current setup uses a local Mosquitto broker without authentication or TLS.
Credentials and certificates will only need to be passed to the Paho
connection options when the secured broker of the Smart Factory is used;
access control lists and TLS termination stay on the broker side.

---

## Design Principles

* Single Responsibility: MQTT session and transport.
* Sparkplug rules stay in the encoder; the publisher applies what it is given.
* No shared mutable state across threads beyond one atomic flag.
* Recovery with a cooldown instead of tight retry loops.

---

## Known Limitations

* There is no `IMqttPublisher` interface. `Gatewayapplication` holds the concrete `Mqttpublisher`, so it cannot be substituted by a mock in unit tests.
* After a reconnection, the births are published on the next poll cycle, not immediately. `publish()` calls made later in the same cycle succeed on the new session, so the DDATA of a second device can be sent before the NBIRTH. Stopping the cycle as soon as a rebirth is pending would close this window.
* The message that triggers a reconnection is dropped; a lost DDATA is sent again once the rebirth is done, because the known state did not advance.
* The reconnection cooldown is a compile-time constant.

---

## Future Extensions

* Username/password and certificate-based authentication.
* TLS connection to the secured Smart Factory broker.
* Configurable cooldown and QoS.
* An `IMqttPublisher` interface to make the application testable without a broker.
