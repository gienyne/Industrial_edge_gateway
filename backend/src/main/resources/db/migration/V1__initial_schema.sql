CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE devices (

    device_id     TEXT  PRIMARY KEY,
    group_id      TEXT NOT NULL,
    edge_node_id  TEXT NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at  TIMESTAMPTZ NOT NULL

);


CREATE SEQUENCE measurements_id_seq

    START WITH 1
    INCREMENT BY 100;



CREATE TABLE measurements (

    id          BIGINT NOT NULL,
    time        TIMESTAMPTZ NOT NULL,
    device_id   TEXT NOT NULL REFERENCES devices(device_id),
    metric_name TEXT NOT NULL,

    datatype    TEXT NOT NULL CHECK (
        datatype IN ('Boolean', 'Integer', 'Double', 'String')
    ),

    unit         TEXT,
    value_bool   BOOLEAN,
    value_int    INTEGER,
    value_double DOUBLE PRECISION,  
    value_string TEXT,

    CHECK (
        (datatype = 'Boolean' AND value_bool   IS NOT NULL AND value_int IS NULL AND value_double IS NULL AND value_string IS NULL)
        OR
        (datatype = 'Integer' AND value_int    IS NOT NULL AND value_bool IS NULL AND value_double IS NULL AND value_string IS NULL)
        OR
        (datatype = 'Double'  AND value_double IS NOT NULL AND value_bool IS NULL AND value_int IS NULL AND value_string IS NULL)
        OR
        (datatype = 'String'  AND value_string IS NOT NULL AND value_bool IS NULL AND value_int IS NULL AND value_double IS NULL)
    )

);


SELECT create_hypertable('measurements', 'time');


CREATE INDEX idx_measurements_device_metric_time ON measurements (device_id, metric_name, time DESC);