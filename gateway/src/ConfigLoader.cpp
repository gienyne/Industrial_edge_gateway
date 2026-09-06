#include "ConfigLoader.h"
#include <fstream>
#include <sstream>
#include <stdexcept>

namespace
{
    /**
     * @brief Converts a string representation into a MetricDataType.
     * 
     * @param value Data type name read from the JSON configuration.
     * @param context Configuration path used to provide a meaningful error message.
     * @return The corresponding MetricDataType.
     * 
     * @throws std::runtime_error If the data type is unknown.
     */
    MetricDataType parseDataType(const std::string& value, const std::string& context)
    {
        if (value == "Boolean") return MetricDataType::Boolean;
        if (value == "Integer") return MetricDataType::Integer;
        if (value == "Double")  return MetricDataType::Double;
        if (value == "String")  return MetricDataType::String;

        throw std::runtime_error("Unknown dataType \"" + value + "\" in " + context);
    }


    /**
     * @brief Retrieves a required field from a JSON object.
     * 
     * @param node JSON object containing the requested field.
     * @param key Name of the required field.
     * @param context Configuration path used to provide a meaningful error message.
     * @return Reference to the requested JSON value.
     * @throws std::runtime_error If the field is missing.
     */
    const nlohmann::json& require(const nlohmann::json& node, const char* key, const std::string& context)
    {
        auto it = node.find(key);

        if (it == node.end()){
            throw std::runtime_error("Missing required field \"" + std::string(key) + "\" in " + context);
        }

        return *it;
    }
}

namespace config
{
    nlohmann::json loadFile(const std::string& path)
    {
        std::ifstream file(path);

        if (!file.is_open()){
            throw std::runtime_error("Could not open configuration file: " + path);
        }

        nlohmann::json root;
        try
        {
            file >> root;
        }
        catch (const nlohmann::json::parse_error& e)
        {
            throw std::runtime_error("Failed to parse configuration file " + path + ": " + e.what());
        }

        return root;
    }

    GatewayApplicationConfig parseGatewayConfig(const nlohmann::json& root)
    {
        const auto& gw = require(root, "gateway", "root");
        const auto& mqtt = require(gw, "mqtt", "gateway");
        const auto& sparkplug = require(gw, "sparkplug", "gateway");

        GatewayApplicationConfig config;

        config.mqttConfig.brokerAddress = require(mqtt, "brokerAddress", "gateway.mqtt").get<std::string>();
        config.mqttConfig.clientId      = require(mqtt, "clientId", "gateway.mqtt").get<std::string>();
        config.mqttConfig.bdSeqFilePath = mqtt.value("bdSeqFilePath", std::string("bdseq.dat"));

        config.encoderConfig.namespaceId = require(sparkplug, "namespaceId", "gateway.sparkplug").get<std::string>();
        config.encoderConfig.groupId     = require(sparkplug, "groupId", "gateway.sparkplug").get<std::string>();
        config.encoderConfig.edgeNodeId  = require(sparkplug, "edgeNodeId", "gateway.sparkplug").get<std::string>();

        config.discoveryWindow = std::chrono::milliseconds(gw.value("discoveryWindowMs", 3000));
        config.deviceTimeout   = std::chrono::milliseconds(gw.value("deviceTimeoutMs", 15000));

        return config;
    }

    ESP32ConnectorConfig parseEsp32Config(const nlohmann::json& root)
    {
        const auto& connectors = require(root, "connectors", "root");
        const auto& esp32 = require(connectors, "esp32", "connectors");

        ESP32ConnectorConfig config;
        config.deviceId      = require(esp32, "deviceId", "connectors.esp32").get<std::string>();
        config.brokerAddress = require(esp32, "brokerAddress", "connectors.esp32").get<std::string>();
        config.topicFilter   = require(esp32, "topicFilter", "connectors.esp32").get<std::string>();

        return config;
    }

    OpcUaConnectorConfig parseOpcUaConfig(const nlohmann::json& root)
    {
        const auto& connectors = require(root, "connectors", "root");
        const auto& opcua = require(connectors, "opcua", "connectors");
        const auto& sourcesJson = require(opcua, "sources", "connectors.opcua");

        OpcUaConnectorConfig config;

        for (const auto& sourceJson : sourcesJson)
        {
            OpcUaSourceConfig source;
            source.endpoint        = require(sourceJson, "endpoint", "connectors.opcua.sources[]").get<std::string>();
            source.deviceId        = require(sourceJson, "deviceId", "connectors.opcua.sources[]").get<std::string>();
            source.username        = require(sourceJson, "username", "connectors.opcua.sources[]").get<std::string>();
            source.password        = require(sourceJson, "password", "connectors.opcua.sources[]").get<std::string>();
            source.certificatePath = require(sourceJson, "certificatePath", "connectors.opcua.sources[]").get<std::string>();
            source.privateKeyPath  = require(sourceJson, "privateKeyPath", "connectors.opcua.sources[]").get<std::string>();

            const auto& metricsJson = require(sourceJson, "metrics", "connectors.opcua.sources[]");
            
            for (const auto& metricJson : metricsJson)
            {
                OpcUaMetricConfig metric;
                metric.nodeId     = require(metricJson, "nodeId", "connectors.opcua.sources[].metrics[]").get<std::string>();
                metric.metricName = require(metricJson, "metricName", "connectors.opcua.sources[].metrics[]").get<std::string>();
                metric.unit       = require(metricJson, "unit", "connectors.opcua.sources[].metrics[]").get<std::string>();

                std::string dataTypeStr = require(metricJson, "dataType", "connectors.opcua.sources[].metrics[]").get<std::string>();
                metric.dataType = parseDataType(dataTypeStr, "connectors.opcua.sources[].metrics[" + metric.metricName + "]");

                source.metrics.push_back(std::move(metric));
            }

            config.sources.push_back(std::move(source));
        }

        return config;
    }
}