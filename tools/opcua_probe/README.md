# OPC UA Probe

The `opcua_probe` is a standalone validation tool developed as part of the
**Industrial Edge Gateway** project.

Its purpose is to validate the OPC UA communication between the gateway
environment and an existing CODESYS-based industrial application before
integrating the communication logic into the final `OpcUaConnector`.

---

## Context

The OPC UA server used during the validation is provided by
**AquaControl**, an existing CODESYS V3.5 SP22 industrial automation
application.

AquaControl simulates an irrigation process and exposes several PLC
variables through its embedded OPC UA server.

Among these variables, the `Level` value represents the current level of
the rainwater storage tank.

The probe uses this real process variable as a test case for OPC UA data
acquisition.

```text
┌──────────────────────────────┐
│        AquaControl           │
│      CODESYS Runtime         │
│                              │
│  PLC Application             │
│       │                      │
│       └── GVL.Level          │
│               │              │
│          OPC UA Server       │
└───────────────┬──────────────┘
                │
                │ OPC UA
                ▼
┌──────────────────────────────┐
│         OPC UA Probe         │
│                              │
│  Connect                     │
│  Authenticate                │
│  Read OPC UA Node            │
│  Convert Value               │
└──────────────────────────────┘
```

The probe therefore provides a small and controlled environment in which
the OPC UA communication can be tested independently from the rest of the
gateway.

---

## Purpose

The probe verifies that the future OPC UA integration can:

- establish a secure connection to the CODESYS OPC UA server;
- use a client certificate and private key;
- authenticate with OPC UA user credentials;
- access a specific OPC UA node;
- read the value exposed by CODESYS;
- correctly convert the received value into a C++ type.

The probe is intentionally kept separate from the final gateway
implementation.

---

## Tested OPC UA Node

The current test reads the following CODESYS variable:

```text
GVL.Level
```

Its OPC UA NodeId is:

```text
ns=4;s=|var|CODESYS Control Win V3 x64.Application.GVL.Level
```

The variable is defined in the AquaControl PLC application as:

| Variable | Type | Description |
|----------|------|-------------|
| `Level` | `REAL` | Rainwater tank level |

For example, a successful test may produce:

```text
Successfully connected to the OPC UA server.
Level (float) = 643.446
```

The actual value depends on the current state of the AquaControl
simulation.

---

## Security

The connection is configured to use:

- OPC UA `SignAndEncrypt`;
- a client certificate;
- the corresponding private key;
- username/password authentication.

The private key and user credentials are sensitive information and must
never be committed to the repository.

Local configuration is therefore separated from the source code.

```text
config/
├── OpcUaConfig.hpp
└── OpcUaSecrets.hpp
```

Certificate and private-key files are also kept outside version control.

---

## Project Structure

```text
opcua_probe/
│
├── CMakeLists.txt
├── opcua_probe.cpp
├── cert_persistence.hpp
│
├── config/
│   ├── OpcUaConfig.hpp
│   └── OpcUaSecrets.hpp
│
├── gateway_cert.der       # local only
├── gateway_key.pem        # local only
│
└── build/
```

The sensitive files above are excluded through `.gitignore`.

---

## Build

From the `tools/opcua_probe` directory:

```powershell
cd build
cmake --build . --config Debug
```

The executable is generated under:

```text
build/Debug/opcua_probe.exe
```

---

## Run

From the `tools/opcua_probe` directory:

```powershell
.\build\Debug\opcua_probe.exe
```

Before running the probe, make sure that:

1. AquaControl is running in CODESYS.
2. The CODESYS OPC UA server is enabled.
3. The OPC UA endpoint is available.
4. The client certificate exists locally.
5. The corresponding private key exists locally.
6. The configured OPC UA credentials are valid.

---

## Expected Result

A successful execution should show:

```text
Successfully connected to the OPC UA server.
Level (float) = <current value>
```

The probe then disconnects cleanly from the OPC UA server.

---

## Role in the Industrial Edge Gateway

This probe represents the **validation stage** of the OPC UA integration.

It is not the final gateway component.

The intended evolution is:

```text
AquaControl
    │
    │ OPC UA
    ▼
OPC UA Probe
    │
    │  Validation
    ▼
OpcUaConnector
    │
    ▼
Industrial Edge Gateway
    │
    ▼
Sensor / Source Data
    │
    ▼
Sparkplug B
```

The successful communication with AquaControl confirms that the required
OPC UA connection, authentication and node-reading mechanisms work before
they are incorporated into the production gateway architecture.

The next step is therefore to transfer this validated OPC UA logic into
the gateway's `OpcUaConnector` while keeping the responsibilities of the
different layers clearly separated.