#ifndef GATEWAYAPPLICATION_H
#define GATEWAYAPPLICATION_H

#include <chrono>
#include <map>
#include <memory>
#include <set>
#include <vector>
#include "IConnector.h"
#include "SparkplugEncoder.h"
#include "Mqttpublisher.h"


/**
 * @brief Configuration of the gateway application.
 * 
 * Contains the configuration required by the gateway components
 * and the initial device discovery period.
 */
struct GatewayApplicationConfig
{
    /**
     * @brief Configuration used by the Sparkplug encoder.
     */
    SparkplugEncodeConfig encoderConfig;

    /**
     * @brief Configuration used by the MQTT publisher.
     */
    MQTTPublisherConfig mqttConfig;

    /**
     * @brief Time for discovering devices at startup.
     * 
     * The default discovery window is 3000 milliseconds.
     */
    std::chrono::milliseconds discoveryWindow{3000};
};


/**
 * @brief Coordinates the main gateway processing flow.
 * 
 * Gatewayapplication connects the gateway components and controls
 * the lifecycle of the Sparkplug Node and its Devices.
 * 
 * The application is responsible for:
 * - initializing connectors
 * - discovering devices
 * - publishing the Sparkplug birth sequence
 * - processing incoming device data
 * - publishing changed metrics
 * - handling newly discovered devices through a rebirth
 * - shutting down the Sparkplug Node cleanly.
 * 
 * The class does not perform source-specific data acquisition,
 * Sparkplug encoding or MQTT communication itself.
 * These responsibilities are delegated to the corresponding components.
 */
class Gatewayapplication
{

    public:

        /**
         * @brief Creates the gateway application.
         * 
         * @param config Gateway configuration.
         * @param connectors Connectors used to collect device data.
         */
        Gatewayapplication(const GatewayApplicationConfig& config, std::vector<std::unique_ptr<IConnector>> connectors);

        /**
         * @brief Initializes the gateway.
         * 
         * Initializes the connectors, discovers available devices,
         * establishes the MQTT connection and publishes the initial  Sparkplug birth sequence.
         * 
         * @return true if initialization succeeds.
         * @return false if an initialization fails.
         */
        bool initialize();

        /**
         * @brief Processes one gateway data cycle.
         * 
         * Collects data from all connectors and publishes
         * the required Sparkplug messages.
         * 
         */
        void pollOnce();

        /**
         * @brief Shuts down the gateway.
         * 
         * Publishes the Node Death message and disconnects from MQTT.
         */
        void shutdown();

    private:
       
        // gateway configuration.
        GatewayApplicationConfig config_;

        // Creates Sparkplug messages from gateway data.
        SparkplugEncoder encoder_;

        // Handles MQTT communication with the broker.
        Mqttpublisher publisher_;

        // Connectors providing data from external sources.
        std::vector<std::unique_ptr<IConnector>> connectors_;

        // Devices for which a DBIRTH has been published. Used to detect newly discovered devices.
        std::set<std::string>birthedDevices_;

        // Latest known state of each device. Used to detect metric changes before publishing DDATA.
        std::map<std::string , DeviceData> lastKnownState_;

        /**
         * @brief Collects device data during the startup discovery period.
         */
        void runDiscoveryWindow();

        /**
         * @brief Publishes a complete Sparkplug birth sequence.
         * 
         * The sequence contains the Node Birth followed by the
         * Device Birth message of every known device.
         * 
         * @return true if the complete sequence succeeds.
         */
        bool publishBirthSequence();

        /**
         * @brief Processes data received from one device.
         * 
         * Only changed metrics are published.
         */
        void handleDeviceData(const DeviceData& data);

        /**
         * @brief Extracts changed metrics from a device update.
         * 
         * @param previous Previous known device state.
         * @param incoming Newly received device state.
         * 
         * @return DeviceData containing only changed metrics.
         */
        DeviceData filterChangedMetrics(const DeviceData& previous, const DeviceData& incoming) const;

};
#endif