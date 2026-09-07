#ifndef OPCUACONNECTOR_H
#define OPCUACONNECTOR_H

#include "IConnector.h"
#include "OpcUaConfig.h"
#include <open62541pp/client.hpp>
#include <open62541pp/node.hpp>
#include <memory>
#include <chrono>


/**
 * @brief Connector responsible for collecting data from OPC UA sources.
 * 
 * Implements the common IConnector interface and manages OPC UA client
 * connections, reconnections, and metric collection for all configured sources.
 */
class OpcUaConnector : public IConnector {

    public:

        /**
         * @brief Creates an OPC UA connector with the given configuration.
         * 
         *  @param config Configuration containing all OPC UA sources and their metrics.
         */
        explicit OpcUaConnector(OpcUaConnectorConfig config);


        /**
         * @brief Destroys the OPC UA connector.
         */
        ~OpcUaConnector() override;


        /**
         * @brief Initializes the configured OPC UA sources.
         * 
         * Loads the client certificates and private keys, creates the OPC UA
         * clients, configures security and authentication, and establishes
         * connections to the configured OPC UA servers.
         * 
         * @return true if at least one OPC UA source was initialized successfully,
         * false otherwise.
         */
        bool initialize() override;


        /**
         * @brief Collects the current values of all configured metrics.
         * 
         * Checks the connection status of each source, attempts reconnection
         * when necessary, and reads the configured OPC UA nodes.
         * 
         * @return A list of DeviceData objects containing the collected metrics.
         */
        std::vector<DeviceData> collectData() override;


        /**
         * @brief Returns the name of the connector.
         * 
         * @return The connector name.
         */
        const char* name() const override;

    private:
    
        /**
         * @brief Runtime state of a configured OPC UA source.
         * 
         * Stores the source configuration together with the credentials,
         * OPC UA client instance, connection state, and reconnection timing.
         */
        struct Source{

            // Configuration of the OPC UA source.
            OpcUaSourceConfig config;

            // Client certificate loaded from the configured certificate file.
            opcua::ByteString certificate;

            // Client private key loaded from the configured private key file.
            opcua::ByteString privateKey;

            // OPC UA client associated with this source.
            std::unique_ptr<opcua::Client> client;

            // Application-level connection state.
            bool connected{false};

            // Time of the last reconnection attempt.
            std::chrono::steady_clock::time_point lastReconnectAttempt{};
        };

        // Configuration provided to the connector.
        OpcUaConnectorConfig config_;

        // Runtime state of all successfully initialized OPC UA sources.
        std::vector<Source> sources_;

        /**
         * @brief Reads one metric from an OPC UA source.
         * 
         * Resolves the configured NodeId, reads its current value, converts it
         * to the configured MetricDataType, and returns it as a generic Metric.
         * 
         * @param source OPC UA source from which the metric is read.
         * @param metricConfig Configuration describing the metric to read.
         * @return The collected metric.
         */
        Metric readMetric(Source& source, const OpcUaMetricConfig& metricConfig);


        bool connectSource(Source& source);
        
};

#endif