"""
Minimal Sparkplug B test listener and validator.

This script listens to Sparkplug B messages, decodes the
Protobuf payload and performs basic validation checks.

It is mainly used to test and validate the Gateway during development.

Usage:

      1. Generate the Python Protobuf classes from proto/sparkplug_b.proto using 
      the protoc executable installed through vcpkg:

      C:\vcpkg\installed\x64-windows\tools\protobuf\protoc.exe --proto_path=proto --python_out=. proto/sparkplug_b.proto

      2. Install the required Python packages:

         pip install paho-mqtt protobuf

      3. Start the listener:

         python listen.py

         
The listener:

- listens to all Sparkplug B messages on spBv1.0/#;
- decodes Sparkplug B Protobuf payloads;
- displays readable metric and message information;
- checks sequence number continuity for each Sparkplug node;
- tracks devices declared through DBIRTH;
- detects DDATA messages received before the corresponding DBIRTH.

"""


import paho.mqtt.client as mqtt
import sparkplug_b_pb2

# MQTT broker configuration.
BROKER = "localhost"
PORT = 1883

# Listen to all Sparkplug B messages.
TOPIC = "spBv1.0/#"

# Sparkplug B DataType values mapped to readable names.
# The values correspond to the DataType enumeration defined by the Sparkplug B specification.
DATATYPE_NAMES = {
    0: "Unknown",
    1: "Int8",
    2: "Int16",
    3: "Int32",
    4: "Int64",
    5: "UInt8",
    6: "UInt16",
    7: "UInt32",
    8: "UInt64",
    9: "Float",
    10: "Double",
    11: "Boolean",
    12: "String",
    13: "DateTime",
    14: "Text",
    15: "UUID",
    16: "DataSet",
    17: "Bytes",
    18: "File",
    19: "Template",
    20: "PropertySet",
    21: "PropertySetList",
}

"""
Validation state per edge_node_id:
_last_seq: Last sequence number received for each Edge Node.
_birthed_devices: Devices for which a DBIRTH has been received since the last NBIRTH.
"""
_last_seq = {}
_birthed_devices = {}


def datatype_name(value : int) -> str :

    """
    Return the readable name of a Sparkplug DataType.
    """
    return DATATYPE_NAMES.get(value, f"Unknown({value})")


def parse_topic(topic : str) :

    """
    Parse a Sparkplug topic:

    Expected format: spBv1.0/<group>/<message_type>/<edge_node_id>/[device_id] "

    Returns the individual topic components or None if the topic is not valid.
    """

    parts = topic.split("/")

    if len(parts) < 4 : 
        return None

    result = {
        "group" : parts[1],
        "message_type" : parts[2],
        "edge_node_id" : parts[3],
        "device_id" : parts[4] if len(parts) > 4 else None,
    }

    return result


def format_metric(metric) -> str :

    """
    Convert a Sparkplug Metric into a readable string.
    """

    # Determine which field of the Metric oneof contains the value.. see Google protobuf file (6.4.1. Google Protocol Buffer Schema [message Payload definition])
    field = metric.WhichOneof("value")

    # Read the value from the corresponding field.
    value = getattr(metric, field) if field else "N/A"

    # Display the DataType name if the datatype field is present.
    datatype = (
        datatype_name(metric.datatype)
        if metric.HasField("datatype")
        else "(omitted)"
    )

    metric_name = metric.name or (f"(unnamed, alias={metric.alias})")

    return f"  - {metric_name} = {value} [{datatype}]"


def validate(info, payload) : 

    """
    Perform basic validation of the received Sparkplug message.

    The checks are informational and do not stop the listener.
    """
    node_key = f"{info['group']}/{info['edge_node_id']}"
    message_type = info["message_type"]

    # NBIRTH starts a new Sparkplug node session.
    if message_type == "NBIRTH" :

        _last_seq[node_key] = ( 
            payload.seq if payload.HasField("seq") 
            else None
            )

        # Reset the list of devices known to have a DBIRTH for this node session.
        _birthed_devices[node_key] = set()

        return

    # NDEATH does not contain a sequence number.
    if message_type == "NDEATH":

        return

    # Expected sequence number is the previous sequence number + 1.
    # The sequence number wraps from 255 back to 0.
    if payload.HasField("seq") : 

        expected = None

        if _last_seq.get(node_key) is not None : 

            expected = ( 
                0 if _last_seq[node_key] == 255 
                else _last_seq[node_key] + 1
                )

        if expected is not None and payload.seq != expected :

            print(f"  [!] Unexpected seq: received {payload.seq}, "f"expected {expected}")


        _last_seq[node_key] = payload.seq

    # A DBIRTH declares that this device exists in the current session.
    if message_type == "DBIRTH" and info["device_id"] :

        _birthed_devices.setdefault(
                node_key, set()
            ).add(info["device_id"])


    # A DDATA should only be received for a device that has
    # previously been declared through DBIRTH in the current session.
    if message_type == "DDATA" and info["device_id"]:

        known_devices = _birthed_devices.get(node_key, set())

        if info["device_id"] not in known_devices :

             print(f"  [!] DDATA received for '{info['device_id']}' " f"without a previous DBIRTH in this session")



def on_connect(client, userdata, flags, reason_code, properties=None) :

    """
    MQTT callback called by Paho when the connection is established.
    """
    print(f"[+] Connected to broker (code: {reason_code})")

    client.subscribe(TOPIC)

    print(f"[+] Listening on: {TOPIC}\n" + "-" * 50)



def on_message(client, userdata, msg) :

    """
    MQTT callback called by Paho when a message is received.
    """

    # Extract information from the MQTT topic.
    info = parse_topic(msg.topic)

    if info is None : 
        return

    # Create an empty Sparkplug Payload object.
    payload = sparkplug_b_pb2.Payload()


    # Decode the Protobuf bytes received from MQTT.
    try :

        payload.ParseFromString(msg.payload)

    except Exception as exc :

        print(f"[!] Protobuf decoding error on {msg.topic}: {exc}")
        return


    print(
        f"\n[{info['message_type']}] {msg.topic}"
        f"(retained={msg.retain})"
        )


    # Display the timestamp if the field is present.
    if payload.HasField("timestamp"):

        print(f"  timestamp = {payload.timestamp}")

    # Display the sequence number if the field is present.
    if payload.HasField("seq") :

        print(f"  seq       = {payload.seq}")

    
    else :

        print("  seq        = (absent)")


    # Display all metrics contained in the Payload.
    if payload.metrics :

        print("  metrics:")

        for metric in payload.metrics:

            print(format_metric(metric))


    # Run the validation checks.
    validate(info, payload)


def build_client() :

    """
     Create and return the MQTT client.
    """

    if hasattr(mqtt, "CallbackAPIVersion") : 

        return mqtt.Client (mqtt.CallbackAPIVersion.VERSION2)

    return mqtt.Client()


if __name__ == "__main__" :

    # Create the MQTT client.
    client = build_client()

    # Register the MQTT callbacks.
    client.on_connect = on_connect
    client.on_message = on_message

    print("[*] Connecting...")

    # Connect to the MQTT broker.
    client.connect(BROKER, PORT, 60)

    # Keep the MQTT client running and processing events.
    client.loop_forever()