#include "OpcUaConnector.h"
#include "TimeUtils.h"
#include <fstream>
#include <chrono>
#include <thread>
#include <iostream>


namespace
{

    /**
     * @brief Loads a binary file into an OPC UA ByteString.
     * 
     * Used to load the client certificate and private key from disk.
     * 
     * @param path Path to the binary file.
     * @return File contents as an OPC UA ByteString.
     * 
     * @throws std::runtime_error If the file cannot be opened.
     */
    opcua::ByteString loadBinaryFile(const std::string& path)
    {
        std::ifstream file(path, std::ios::binary);

        if(!file){
            throw std::runtime_error("Could not open file: " + path);
        }

        std::vector<uint8_t> buffer ((std::istreambuf_iterator<char>(file)), std::istreambuf_iterator<char>());

        return opcua::ByteString(buffer.begin(), buffer.end());
    }

    // Minimum delay between two consecutive reconnection attempts.
    constexpr auto ReconnectCooldown = std::chrono::seconds(5);

}


OpcUaConnector::OpcUaConnector(OpcUaConnectorConfig config) : config_(std::move(config))
{

}


bool OpcUaConnector::initialize()
{
    sources_.clear();

    for(const auto& srcConfig : config_.sources){

        try{

            Source source;
            source.config = srcConfig;

            source.certificate = loadBinaryFile(srcConfig.certificatePath);
            source.privateKey  = loadBinaryFile(srcConfig.privateKeyPath);

            opcua::ClientConfig clientConfig(source.certificate, source.privateKey, {});

            clientConfig.setSecurityMode(opcua::MessageSecurityMode::SignAndEncrypt);

            auto* rawConfig = clientConfig.handle();

            UA_String_clear(&rawConfig->clientDescription.applicationUri);

            rawConfig->clientDescription.applicationUri = UA_STRING_ALLOC ("urn:industrial-edge-gateway:opcua-probe");

            clientConfig.setUserIdentityToken(opcua::UserNameIdentityToken(srcConfig.username, srcConfig.password));

            source.client = std::make_unique<opcua::Client>(std::move(clientConfig));

            source.client->connect(srcConfig.endpoint);

            // Allow the OPC UA client to process connection and session events.
            for(int i = 0 ; i < 30; i++){

                source.client->runIterate(10);

                std::this_thread::sleep_for(std::chrono::milliseconds(20));

            }

            source.connected = true;
            sources_.push_back(std::move(source));

            std::cout << "[OpcUaConnector] Session ready for " << srcConfig.deviceId << "\n" << std::flush;
        }
        catch(const std::exception& e)
        {
            std::cerr << "[OpcUaConnector] Connection failed for " << srcConfig.deviceId << ": " << e.what() << "\n" << std::flush;
        }
    }

    if(!sources_.empty()){
        return sources_[0].connected;
    }

    return false;
}


Metric OpcUaConnector::readMetric(OpcUaConnector::Source& source, const OpcUaMetricConfig& metricConfig)
{
    Metric metric;

    metric.name = metricConfig.metricName;
    metric.datatype = metricConfig.dataType;
    metric.unit = metricConfig.unit;
    metric.timestamp = nowMillis();

    // Create a node reference from the configured NodeId.
    opcua::Node node(*source.client, opcua::NodeId::parse(metricConfig.nodeId));

    // Read the current value from the OPC UA server.
    auto variant = node.readValue();

    if(variant.empty()){
        throw std::runtime_error("Node or value not found on the CODESYS server");
    }

    switch(metricConfig.dataType){

        case MetricDataType::Boolean:

           metric.value = variant.scalar<bool>();
           break;

        case MetricDataType::Double:
           
           if(variant.isType<float>()){
             metric.value =  static_cast<double>(variant.scalar<float>());
           }
           else if(variant.isType<double>()){
             metric.value = variant.scalar<double>();
           }
           else if(variant.isType<int32_t>()){
             metric.value = static_cast<double>(variant.scalar<int32_t>());
           }
           else {
             throw std::runtime_error("Unable to convert OPC UA value to Double");
           }

           break;

        case MetricDataType::Integer:
            
           metric.value = variant.scalar<int32_t>();
           break;

        
        case MetricDataType::String:

           metric.value = std::string(variant.scalar<opcua::String>());
           break;
    }

    return metric;
}


std::vector<DeviceData> OpcUaConnector::collectData()
{
    std::vector<DeviceData> result;

    for (auto& source : sources_){

        if(!source.client){
            continue;
        }

        // Check the connection state and attempt reconnection if necessary.
        if (!source.connected || !source.client->isConnected()){

            auto now = std::chrono::steady_clock::now();

            if (now - source.lastReconnectAttempt < ReconnectCooldown){
                continue;
            }

            source.lastReconnectAttempt = now;

            try{

                source.client->connect(source.config.endpoint);
                source.connected = true;

                std::cout << "[OpcUaConnector] Reconnection successful for " << source.config.deviceId << "\n" << std::flush;
            }
            catch (const std::exception& e)
            {
                std::cerr << "[OpcUaConnector] Reconnection failed for " << source.config.deviceId << ": " << e.what() << "\n" << std::flush;
                continue;
            }
        }

        try{
            // Process pending OPC UA communication and client events.
            source.client->runIterate(10);
        }
        catch (const std::exception& e)
        {
            std::cerr << "[OpcUaConnector] runIterate failed for " << source.config.deviceId << ": " << e.what() << "\n" << std::flush;
            source.connected = false;
            continue;
        }

        DeviceData data;

        data.deviceId = source.config.deviceId;
        data.timestamp = nowMillis();

        for (const auto& metricConfig : source.config.metrics){

            try{

                source.client->runIterate(5);

                data.metrics.push_back(readMetric(source, metricConfig));

            }
            catch(const std::exception& e)
            {
                std::cerr << "[Metric Read Error] " << metricConfig.metricName << " (" << metricConfig.nodeId << "): " << e.what() << "\n" << std::flush;
            }
        }

        if(!data.metrics.empty()){
            result.push_back(std::move(data));
        }


    }

    return result;
}


const char* OpcUaConnector::name() const
{
    return "OpcUaConnector";
}

OpcUaConnector::~OpcUaConnector() = default;