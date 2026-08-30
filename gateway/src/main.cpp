#include <atomic>
#include <csignal>
#include <chrono>
#include <iostream>
#include <memory>
#include <thread>
#include <vector>

#include "ESP32Connector.h"
#include "Gatewayapplication.h" 

namespace 
{
    std::atomic<bool> running{true};

    void handleSignal(int){
        running = false;
    }

}

int main(){

   std::cout << "Industrial Edge Gateway starting..." << std::endl;

   std::signal(SIGINT, handleSignal);
   std::signal(SIGTERM, handleSignal);

   ESP32ConnectorConfig esp32Config;
   esp32Config.deviceId = "esp32-connector";
   esp32Config.brokerAddress = "tcp://localhost:1883"; //"tcp://broker.hivemq.com:1883";
   esp32Config.topicFilter = "raw/+";


   std::vector<std::unique_ptr<IConnector>> connectors;
   connectors.push_back(std::make_unique<ESP32Connector>(esp32Config));


   GatewayApplicationConfig config;
   config.encoderConfig.namespaceId = "spBv1.0";
   config.encoderConfig.groupId = "SFM";
   config.encoderConfig.edgeNodeId = "IndustrialEdgeGateway";
   config.mqttConfig.brokerAddress = "tcp://localhost:1883";   //"tcp://broker.hivemq.com:1883";
   config.mqttConfig.clientId = "industrial-edge-gateway";


   Gatewayapplication app(config, std::move(connectors));

   if(!app.initialize()){
    
    std::cerr << "GatewayApplication initialization failed" << std::endl;
    return 1;

   }

   std::cout << "Gateway running. Press Ctrl+C to exit." << std::endl;

   while(running){

    app.pollOnce();
    std::this_thread::sleep_for(std::chrono::milliseconds(500));

   }

   std::cout << "Shutting down..." << std::endl;
   app.shutdown();

    return 0;
} 