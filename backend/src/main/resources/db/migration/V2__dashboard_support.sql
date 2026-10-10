ALTER TABLE devices
    ADD COLUMN status TEXT NOT NULL DEFAULT 'UNKNOWN'
    CHECK (status IN ('ONLINE', 'OFFLINE', 'UNKNOWN'));


CREATE TABLE device_metrics(

    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id   TEXT NOT NULL REFERENCES devices(device_id),
    metric_name TEXT NOT NULL,
    datatype    TEXT NOT NULL CHECK (datatype IN ('Boolean', 'Integer', 'Double', 'String')),
    unit        TEXT,
    UNIQUE (device_id, metric_name)

);


CREATE TABLE events (

    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    time         TIMESTAMPTZ NOT NULL,
    type         TEXT NOT NULL CHECK (type IN ('EDGE_NODE_ONLINE', 'EDGE_NODE_OFFLINE', 'DEVICE_ONLINE', 'DEVICE_OFFLINE', 'REBIRTH_REQUESTED')),
    group_id     TEXT NOT NULL,
    edge_node_id TEXT NOT NULL,
    device_id    TEXT REFERENCES devices(device_id),
    message      TEXT NOT NULL

);


CREATE TABLE edge_nodes (

    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id     TEXT NOT NULL,
    edge_node_id TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'UNKNOWN' CHECK (status IN ('ONLINE', 'OFFLINE', 'UNKNOWN')),
    bd_seq       BIGINT,
    UNIQUE (group_id, edge_node_id)

);


CREATE INDEX idx_events_time
   ON events (time DESC);


CREATE INDEX idx_events_device_time
   ON events (device_id, time DESC);


INSERT INTO device_metrics (device_id, metric_name, datatype, unit)
SELECT DISTINCT ON (device_id, metric_name)
   device_id,
   metric_name,
   datatype,
   unit
FROM measurements
ORDER BY device_id, metric_name, time DESC;