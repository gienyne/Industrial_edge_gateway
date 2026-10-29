ALTER TABLE events DROP CONSTRAINT IF EXISTS events_type_check;


ALTER TABLE events
   ADD CONSTRAINT events_type_check CHECK (type IN (

       'EDGE_NODE_ONLINE',
       'EDGE_NODE_OFFLINE',
       'DEVICE_ONLINE',
       'DEVICE_OFFLINE',
       'REBIRTH_REQUESTED',
       'SEQUENCE_GAP'
       
   ));