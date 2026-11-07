#ifndef ESP32CONNECTOR_H
#define ESP32CONNECTOR_H

#include <atomic>
#include <chrono>
#include <map>
#include <string>
#include <mutex>
#include <mqtt/async_client.h>
#include "IConnector.h"

/**
 * @brief Configuration for the ESP32 MQTT connector
 */
struct ESP32ConnectorConfig{
    std::string deviceId;
    std::string brokerAddress;
    std::string topicFilter;
};


/**
 *  @brief Connector for receiving data from ESP32 devices over MQTT
 */
class ESP32Connector : public IConnector, public virtual mqtt::callback
{

    public:

        /**
         * @brief Create an ESP32 connector with the given configuration.
         */
        explicit ESP32Connector(const ESP32ConnectorConfig& config);

        bool initialize() override;

        std::vector<DeviceData> collectData() override;

        const char* name() const override;

        /**
         * @brief Handle an incoming MQTT message.
         */
        void message_arrived(mqtt::const_message_ptr msg) override;

        /**
         * @brief Handle a lost MQTT connection.
         */
        void connection_lost(const std::string& lst) override;

    private:
        
        ESP32ConnectorConfig config_;
        mqtt::async_client mqttClient_;

        // Protects access to pending MQTT payloads.
        std::mutex mutex_;

        // Stores received payloads until they are processed.
        std::map<std::string, std::string> pendingPayloads_;
        
        // Connection state shared between Paho's callback thread and
        // the Gateway's main thread.
        std::atomic<bool> connected_{false};

        // Timestamp of the most recent reconnection attempt.
        std::chrono::steady_clock::time_point lastReconnectAttempt_{};

        /**
         * @brief Connects to the MQTT broker and subscribes to the configured topic.
         * 
         * This method is used both for the initial connection and for
         * automatic reconnection attempts.
         * 
         * @return true if the connector is connected and subscribed successfully.
         */
        bool connectMqtt();


        /**
         * @brief Convert an MQTT JSON payload into DeviceData.
         */
        DeviceData parsePayload(const std::string& deviceId, const std::string& payload);

        /**
         * @brief Extract the device ID from an MQTT topic.
         */
        std::string extractDeviceId(const std::string& topic) const;
};

#endif