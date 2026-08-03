# Development Workflow

## Branches

main

Stable releases only.

---

develop

Integration branch.

All completed features are merged into develop first.

---

feature/<name>

One branch for one feature.

Examples

feature/sprint1-sensors

feature/sparkplug-encoder

feature/backend

feature/database

feature/dashboard

---

## Commit Messages

Examples

feat: add DHT11 driver

feat: implement Sparkplug encoder

fix: correct MQTT topic

docs: update architecture

refactor: simplify encoder

---

## General Rules

- keep commits small
- test before merging
- document important changes
- avoid committing generated files