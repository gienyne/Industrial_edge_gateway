"""
Publishes an NCMD Node Control/Rebirth=true command to a Sparkplug B Edge Node.

Usage:

   1. The sparkplug_b_pb2 bindings must already be generated. 
   See listen.py for the protoc command.

   2. Run: python send_ncmd_rebirth.py

"""


import time
import paho.mqtt.client as mqtt 
import sparkplug_b_pb2


# MQTT broker configuration
BROKER = "localhost"
PORT = 1883

# Sparkplug group and Edge Node identifiers.
GROUP = "SFM"
EDGE_NODE_ID = "IndustrialEdgeGateway"

# NCMD topic used to send Node Control commands to the Edge Node.
TOPIC = f"spBv1.0/{GROUP}/NCMD/{EDGE_NODE_ID}"

# Sparkplug requires NCMD messages to be published with QoS 0.
# This is independent from the QoS used by the Gateway when subscribing.
PUBLISH_QOS = 0


def build_rebirth_payload() -> bytes:

    """
    Builds the Protobuf payload for a Node Control/Rebirth=true command.

    NCMD messages do not contain a sequence number.
    The datatype is included here to explicitly identify the value as Boolean.
    """
 
    payload = sparkplug_b_pb2.Payload()
    payload.timestamp = int(time.time() * 1000)

    metric = payload.metrics.add()
    metric.name = "Node Control/Rebirth"
    metric.timestamp = payload.timestamp
    metric.datatype = 11  # Boolean
    metric.boolean_value = True

    # Serialize the Protobuf message into bytes for MQTT transmission.
    return payload.SerializeToString()


def main():

    client = (
        mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
        if hasattr(mqtt, "CallbackAPIVersion")
        else mqtt.Client()
    )

    # Connect to the MQTT broker and start the network loop.
    client.connect(BROKER, PORT, 60)
    client.loop_start()

    # Build the binary Sparkplug payload.
    payload_bytes = build_rebirth_payload()

    # Publish the NCMD Rebirth command.
    result = client.publish (TOPIC, payload_bytes, qos=PUBLISH_QOS, retain=False)

    # Wait until the message has been published.
    result.wait_for_publish()

    print(f"[+] NCMD Node Control/Rebirth=true published on: {TOPIC}")

    # Stop the MQTT network loop and disconnect cleanly.
    client.loop_stop()
    client.disconnect()


if __name__ == "__main__":
    main()
    