#ifndef CONFIGLOADER_H
#define CONFIGLOADER_H

#include <nlohmann/json.hpp>
#include <string>
#include "Gatewayapplication.h"
#include "ESP32Connector.h"
#include "OpcUaConfig.h"

/**
 * @brief Provides functions for loading and parsing the gateway configuration.
 * 
 * The configuration is loaded from a JSON file and parsed into the
 * configuration structures required by the gateway and its connectors.
 * 
 * Each parsing function is responsible for one specific configuration
 * section and returns the corresponding configuration object.
 */
namespace config
{

    /**
     * @brief Loads a JSON configuration file.
     * 
     * Opens the specified file, parses its contents, and returns
     * the resulting JSON document.
     * 
     * @param path Path to the JSON configuration file.
     * @return The parsed JSON document.
     */
    nlohmann::json loadFile(const std::string& path);


    /**
     * @brief Parses the gateway configuration.
     * 
     * Reads the "gateway" section of the JSON document and creates
     * the corresponding GatewayApplicationConfig object.
     * 
     * @param root Root JSON document containing the gateway configuration.
     * @return Parsed gateway configuration.
     */
    GatewayApplicationConfig parseGatewayConfig(const nlohmann::json& root);

    /**
     * @brief Parses the ESP32 connector configuration
     * 
     * Reads the "connectors.esp32" section of the JSON document and
     * creates the corresponding ESP32ConnectorConfig object.
     * 
     * @param root Root JSON document containing the connector configuration.
     * @return Parsed ESP32 connector configuration.
     */
    ESP32ConnectorConfig parseEsp32Config(const nlohmann::json& root);

    /**
     * @brief Parses the OPC UA connector configuration.
     * 
     * Reads the "connectors.opcua" section of the JSON document and
     * creates the corresponding OpcUaConnectorConfig object.
     * 
     * @param root Root JSON document containing the connector configuration.
     * @return Parsed OPC UA connector configuration.
     */
    OpcUaConnectorConfig parseOpcUaConfig(const nlohmann::json& root);
}

#endif