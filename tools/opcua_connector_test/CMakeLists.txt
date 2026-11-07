cmake_minimum_required(VERSION 3.15)
project(OpcUaConnectorTest LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

find_package(open62541pp CONFIG REQUIRED)
find_package(nlohmann_json CONFIG REQUIRED)

add_executable(opcua_connector_test
    test_main.cpp
    ../../gateway/src/OpcUaConnector.cpp
)

target_include_directories(opcua_connector_test PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../gateway/include
)

target_link_libraries(opcua_connector_test PRIVATE
open62541pp::open62541pp
nlohmann_json::nlohmann_json)