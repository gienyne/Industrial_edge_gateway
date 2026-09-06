#ifndef OPCUACONFIG_H
#define OPCUACONFIG_H

#include <string>
#include <vector>
#include "Metric.h"

/**
 * @brief Configuration for a single OPC UA metric.
 * 
 * Defines which OPC UA node is read and how the resulting value
 * is represented as a generic Metric.
 */
struct OpcUaMetricConfig {

    // OPC UA NodeId used to read the value from the server.
    std::string nodeId;

    // Name assigned to the metric in the gateway.
    std::string metricName;

    // Expected data type of the metric value.
    MetricDataType dataType;   

    // Unit associated with the metric value.
    std::string unit;
};


/**
 * @brief Configuration for a single OPC UA data source.
 * 
 * Contains the connection parameters and the list of metrics
 * to be collected from the OPC UA server.
 */
struct OpcUaSourceConfig {

    // OPC UA server endpoint
    std::string endpoint;

    // Identifier used by the gateway to identify the source device.
    std::string deviceId;

    // Username used for OPC UA authentication.
    std::string username;

    // Password used for OPC UA authentication.
    std::string password;

    // Path to the client certificate file.
    std::string certificatePath;

    // Path to the client private key file.
    std::string privateKeyPath;

    // List of metrics to collect from this OPC UA source.
    std::vector<OpcUaMetricConfig> metrics;
};


/**
 * @brief Configuration for the OPC UA connector.
 * 
 * Contains all OPC UA sources configured for the gateway.
 */
struct OpcUaConnectorConfig {

    // List of OPC UA data sources handled by the connector.
    std::vector<OpcUaSourceConfig> sources;
    
};

#endif