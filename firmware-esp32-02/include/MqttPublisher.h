#ifndef MQTTPUBLISHER_H
#define MQTTPUBLISHER_H

#include <Arduino.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include "SensorConnector.h"

/**
 * @brief Handles WiFi connection, MQTT connection and data publishing.
 */
class MqttPublisher
{

    public:
        
        /**
         * @brief Create an MQTT publisher.
         */
        MqttPublisher();

        /**
         * @brief Initialize WiFi and MQTT.
         */
        bool initialize();

        /**
         * @brief Check and restore WiFi and MQTT connections.
         */
        void ensureConnection();

        /**
         * @brief Publish collected sensor data.
         * @param data SourceData containing the sensor readings
         */
        bool publish(const SourceData& data);

    private:

        WiFiClient wifiClient_;
        PubSubClient mqttClient_;

        void connectWiFi();
        void connectMqtt();
};

#endif