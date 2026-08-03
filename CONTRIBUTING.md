# Development Workflow

## Git Branches

### main

Always contains the latest stable version.

---

### develop

Integration branch.

Completed features are merged here before being released to **main**.

---

### feature/<name>

Each feature is developed in its own branch.

Examples:

```
feature/sprint1-sensors
feature/sparkplug-encoder
feature/java-gateway
feature/influxdb
feature/dashboard
feature/opcua-connector
```

---

## Commit Convention

Use small and meaningful commits.

Examples:

```
feat: add DHT11 driver
feat: implement Sparkplug encoder
feat: publish NBIRTH message
fix: correct MQTT topic
refactor: simplify sensor interface
docs: update architecture
```

---

## General Rules

- Keep commits focused on one change.
- Test before merging.
- Document architectural decisions.
- Never commit generated files.
- Keep the project modular.
- Extend the software instead of rewriting existing components.
