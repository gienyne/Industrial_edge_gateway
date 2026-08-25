cmake_minimum_required(VERSION 3.15)
project(IndustrialEdgeGateway)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(PahoMqttCpp CONFIG REQUIRED)
find_package(nlohmann_json CONFIG REQUIRED)
find_package(protobuf CONFIG REQUIRED)

set(PROTO_FILE "${CMAKE_CURRENT_SOURCE_DIR}/proto/sparkplug_b.proto")

add_executable(gateway 
src/main.cpp
src/ESP32Connector.cpp
src/SparkplugEncoder.cpp
src/Gatewayapplication.cpp
src/Mqttpublisher.cpp
${PROTO_FILE}
)

protobuf_generate(TARGET gateway LANGUAGE cpp)

target_include_directories(gateway PRIVATE include 
${CMAKE_CURRENT_BINARY_DIR}
${CMAKE_CURRENT_BINARY_DIR}/gateway.dir/Debug
)

target_compile_definitions(gateway PRIVATE PAHO_MQTTPP_IMPORTS)

target_link_libraries(gateway PRIVATE PahoMqttCpp::paho-mqttpp3-shared 
nlohmann_json::nlohmann_json
protobuf::libprotobuf
)