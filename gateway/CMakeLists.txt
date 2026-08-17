cmake_minimum_required(VERSION 3.15)
project(IndustrialEdgeGateway)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(PahoMqttCpp CONFIG REQUIRED)
find_package(nlohmann_json CONFIG REQUIRED)

add_executable(gateway 
src/main.cpp
src/ESP32Connector.cpp
)

target_include_directories(gateway PRIVATE include)

target_compile_definitions(gateway PRIVATE PAHO_MQTTPP_IMPORTS)
target_link_libraries(gateway PRIVATE 
PahoMqttCpp::paho-mqttpp3-shared
nlohmann_json::nlohmann_json
)