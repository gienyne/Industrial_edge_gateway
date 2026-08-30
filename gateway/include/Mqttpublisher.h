#ifndef MQTTPUBLISHER_H
#define MQTTPUBLISHER_H

#include <atomic>
#include <string>
#include <mqtt/async_client.h>
#include "SparkplugEncoder.h"
#include "SparkplugPayload.h"
#include "BdSeqManager.h"




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


    std::string bdSeqFilePath = "bdseq.dat";

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
class Mqttpublisher : public virtual mqtt::callback
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

        /**
         * @brief Checks whether a Node Rebirth request was received.
         * 
         * Consumes the pending request so it is handled only once.
         * 
         * @return true if a Rebirth request was pending, false otherwise.
         */
        bool consumeRebirthRequest();


        /**
         * @brief Handles an incoming MQTT message.
         * 
         * Processes Node Control commands received from the broker.
         * 
         * @param msg MQTT message received by the client.
         */
        void message_arrived(mqtt::const_message_ptr msg) override;


        /**
         * @brief Handles the loss of the MQTT connection.
         * 
         * @param lst Description of the connection loss.
         */
        void connection_lost(const std::string& lst) override;

    private:

        // MQTT broker and client configuration.
        MQTTPublisherConfig config_;

        // Reference to the Sparkplug encoder.
        SparkplugEncoder& encoder_;

        // Eclipse Paho asynchronous MQTT client.
        mqtt::async_client mqttClient_;

        BdSeqManager bdSeqManager_;

        // Indicates whether a Node Rebirth request is pending.
        std::atomic<bool> rebirthRequested_ {false};
};


#endif