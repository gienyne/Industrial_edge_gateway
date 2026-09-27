cmake_minimum_required(VERSION 3.15)

project(OpcUaProbe LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(open62541pp CONFIG REQUIRED)

add_executable(opcua_probe
    opcua_probe.cpp
)

target_link_libraries(opcua_probe
    PRIVATE
        open62541pp::open62541pp
)