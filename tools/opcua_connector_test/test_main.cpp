#include "OpcUaConnector.h"

#include <chrono>
#include <fstream>
#include <iostream>
#include <nlohmann/json.hpp>
#include <stdexcept>
#include <thread>
#include <variant>


namespace
{
    OpcUaSourceConfig loadTestConfig(const std::string& path)
    {
        std::ifstream file(path);

        if (!file){
            throw std::runtime_error("Could not open test configuration file: " + path);
        }

        nlohmann::json config;

        try
        {
            file >> config;
        }
        catch(const nlohmann::json::parse_error& e)
        {
            throw std::runtime_error("Failed to parse test configuration file: " + std::string(e.what()));
        }

        OpcUaSourceConfig source;

        source.endpoint = config.at("endpoint").get<std::string>();
        source.deviceId = config.at("deviceId").get<std::string>();
        source.username = config.at("username").get<std::string>();
        source.password = config.at("password").get<std::string>();
        source.certificatePath = config.at("certificatePath").get<std::string>();
        source.privateKeyPath = config.at("privateKeyPath").get<std::string>();

        return source;

    }
}

int main(){

    std::cout << "[TEST] Starting OPC UA connector test..." << std::endl;

    OpcUaSourceConfig aquaControlConfig;

    try{
        aquaControlConfig = loadTestConfig("../test_config.json");
    }
    catch(const std::exception& e)
    {
        std::cerr << "[TEST] Configuration error: " << e.what() << std::endl;

        return 1;
    }

    aquaControlConfig.metrics = {

        {
            "ns=4;s=|var|CODESYS Control Win V3 x64.Application.GVL.Level", "tankLevel", MetricDataType::Double, "L"
        },

        {
            "ns=4;s=|var|CODESYS Control Win V3 x64.Application.GVL.Pumpe", "pumpActive", MetricDataType::Boolean, "-"
        },

        {
            "ns=4;s=|var|CODESYS Control Win V3 x64.Application.GVL.Ventil", "valveActive", MetricDataType::Boolean, "-"
        },

        {
            "ns=4;s=|var|CODESYS Control Win V3 x64.Application.GVL.Verbrauch", "waterConsumption", MetricDataType::Double, "L"
        },

        {
            "ns=4;s=|var|CODESYS Control Win V3 x64.Application.GVL.Regen", "rainSimActive", MetricDataType::Boolean, "-"
        }
    };

    OpcUaConnectorConfig config{{aquaControlConfig}};
    OpcUaConnector connector(config);

    if (!connector.initialize()){
        std::cerr << "[TEST] Connector initialization failed." << std::endl;
        return 1;
    }

    std::cout << "[TEST] Connector initialized successfully." << std::endl;

    constexpr int Iterations = 20;

    for (int i = 1; i <= Iterations; ++i){

        std::cout << "\n-- [Read " << i << "/" << Iterations << "] --" << std::endl;

        auto devices = connector.collectData();

        if(devices.empty()){
            std::cout << "[TEST] No data collected during this iteration." << std::endl;
        }

        for (const auto& device : devices){

            std::cout << "Device: " << device.deviceId << " | timestamp=" << device.timestamp << std::endl;

            for(const auto& metric : device.metrics){

                std::cout << "  -> " << metric.name << " = ";

                std::visit([](auto&& value){
                    std::cout << std::boolalpha << value;
                    }, metric.value
                );

                std::cout << " " << metric.unit << std::endl;
                }
            }
    }

    std::this_thread::sleep_for(std::chrono::seconds(1));

    std::cout << "\n[TEST] Test completed successfully after "  << Iterations << " iterations." << std::endl;

    return 0;
}