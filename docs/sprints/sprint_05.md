# Sprint 5 – Integration of Smart Factory Equipment

## Objective

Integrate real industrial equipment from the Smart Factory Mittelhessen into the gateway and demonstrate that a new machine can be added through configuration without modifying the gateway core.


### Machine Integration

- [x] Obtain OPC UA access information for the machine
- [x] Make the security mode configurable per source (`None`, `SignAndEncrypt`)
- [x] Add the  machine through configuration
- [x] Map process variables to gateway metrics
- [x] Display machine status, current values and history in the dashboard
- [ ] Define informational limits for the temperature zones

### Hardening

- [x] Validate OPC UA server certificates using a trust store


## Success Criteria

- [x] Smart Factory machine delivers live data through the gateway to the dashboard
- [x] machine should be added without changes to the gateway core
- [x] Disconnection and reconnection behavior is validated
