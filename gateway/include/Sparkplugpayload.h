#ifndef SPARKPLUGPAYLOAD_H
#define SPARKPLUGPAYLOAD_H

#include <string>
#include <vector>
#include <cstdint>

using MqttTopic = std::string;
using BinaryPayload = std::vector<uint8_t>;

/**
 * @brief Represents a complete Sparkplug message ready for MQTT transport.
 * 
 * Contains the MQTT topic, serialized Sparkplug B payload and the MQTT
 * transport parameters required for publication.
 */
struct SparkplugPayload
{
    /**
     * @brief Target MQTT topic.
     */
    MqttTopic topic;

    /**
     * @brief Serialized Sparkplug B payload.
     * 
     * Contains the Protobuf-encoded Sparkplug message as raw bytes.
     */
    BinaryPayload payload; 

    /**
     * @brief MQTT Quality of Service level used for publication.
     */
    int qos;

    /**
     * @brief MQTT retain flag used for publication.
     */
    bool retain;
};


#endif