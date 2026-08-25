#ifndef MQTTPUBLISHER_H
#define MQTTPUBLISHER_H

#include <string>
#include <mqtt/async_client.h>
#include "SparkplugEncoder.h"
#include "SparkplugPayload.h"


/**
 * @brief Configuration required by the MQTT publisher.
 */
struct MQTTPublisherConfig 
{
    /**
     * @brief Address of the MQTT broker.
     */
    std::string brokerAddress;

    /**
     * @brief MQTT client identifier used by the gateway.
     */
    std::string clientId;
};


/**
 * @brief Handles MQTT communication for the gateway.
 * 
 * Mqttpublisher is responsible for establishing the MQTT connection,
 * publishing prepared SparkplugPayload objects and disconnecting
 * from the broker.
 * 
 * Sparkplug messages are created by SparkplugEncoder.
 * This class only handles their MQTT transport.
 */
class Mqttpublisher 
{
    
    public:

        /**
         * @brief Creates an MQTT publisher.
         * 
         * @param config MQTT broker and client configuration.
         * @param encoder Sparkplug encoder used for session-related messages.
         */
        Mqttpublisher(const MQTTPublisherConfig& config, SparkplugEncoder& encoder);

        /**
         * @brief Initializes the MQTT connection.
         * 
         * @return true if the connection succeeds.
         * @return false if the connection fails.
         */
        bool initialize();

        /**
         * @brief Publishes a prepared Sparkplug payload.
         * 
         * @param payload Sparkplug payload to send to the broker.
         * 
         * @return true if the publication succeeds.
         * @return false if the publication fails.
         */
        bool publish(const SparkplugPayload& payload);
    
        /**
         * @brief Disconnects cleanly from the MQTT broker.
         * 
         * @return true if the disconnection succeeds.
         * @return false if the disconnection fails.
         */
        bool disconnect();

    private:

        // MQTT broker and client configuration.
        MQTTPublisherConfig config_;

        // Reference to the Sparkplug encoder.
        SparkplugEncoder& encoder_;

        // Eclipse Paho asynchronous MQTT client
        mqtt::async_client mqttClient_;
};


#endif