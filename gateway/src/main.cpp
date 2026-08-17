#include <iostream>
#include <thread>
#include <chrono>

#include "ESP32Connector.h"

int main(){

    std::cout << "Industrial Edge Gateway starting..." << std::endl;

    ESP32ConnectorConfig config;
    config.deviceId = "esp32-connector";
    config.brokerAddress = "tcp://broker.hivemq.com:1883";
    config.topicFilter = "raw/+";

    ESP32Connector connector(config);

    if(connector.initialize()){
        std::cout << "ESP32Connector initialized" << std::endl;
    }
    else{
        std::cerr << "ESP32Connector initialization failed" << std::endl;
        return 1;
    }

    std::cout << "Collecting data. Press Ctrl+C to exit." << std::endl;

    while(true){

        std::vector<DeviceData> allData = connector.collectData();

        for(const auto& data : allData){

            std::cout << "DeviceData received for '" << data.deviceId << "':" << std::endl;

            for(const auto& metric : data.metrics){
                std::cout << "  " << metric.name << " = ";
                std::visit([](const auto& v) { std::cout << v; }, metric.value);
                std::cout << " " << metric.unit << std::endl;
            }
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(500));
    }
    return 0;
} 