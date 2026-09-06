#include <atomic>
#include <csignal>
#include <chrono>
#include <iostream>
#include <memory>
#include <thread>
#include <vector>

#include "ESP32Connector.h"
#include "OpcUaConnector.h"
#include "Gatewayapplication.h"
#include "ConfigLoader.h"

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

   nlohmann::json root;
   try
   {
       root = config::loadFile("config/config.json");
   }
   catch (const std::exception& e)
   {
       std::cerr << "Configuration error: " << e.what() << std::endl;
       return 1;
   }

   std::vector<std::unique_ptr<IConnector>> connectors;

   try
   {
       connectors.push_back(std::make_unique<ESP32Connector>(config::parseEsp32Config(root)));
       connectors.push_back(std::make_unique<OpcUaConnector>(config::parseOpcUaConfig(root)));
   }
   catch (const std::exception& e)
   {
       std::cerr << "Connector configuration error: " << e.what() << std::endl;
       return 1;
   }

   GatewayApplicationConfig gatewayConfig;
   try
   {
       gatewayConfig = config::parseGatewayConfig(root);
   }
   catch (const std::exception& e)
   {
       std::cerr << "Gateway configuration error: " << e.what() << std::endl;
       return 1;
   }

   Gatewayapplication app(gatewayConfig, std::move(connectors));

   if(!app.initialize()){
    std::cerr << "GatewayApplication initialization failed" << std::endl;
    return 1;
   }

   std::cout << "Gateway running. Press Ctrl+C to exit." << std::endl;

   while(running){
    try{
        app.pollOnce();
    }
    catch (const std::exception& e){
        std::cerr << "pollOnce() failed: " << e.what() << std::endl;
    }

    std::this_thread::sleep_for(std::chrono::milliseconds(500));
   }

   std::cout << "Shutting down..." << std::endl;
   app.shutdown();

    return 0;
}