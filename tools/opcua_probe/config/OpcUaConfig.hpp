#pragma once

namespace OpcUaConfig
{
    // OPC UA server endpoint.
    constexpr const char* SERVER_URL = "opc.tcp://127.0.0.1:4840";

    // Application URI used by the OPC UA client.
    constexpr const char* APPLICATION_URI = "urn:industrial-edge-gateway:opcua-probe";

    // Client certificate file.
    constexpr const char* CLIENT_CERTIFICATE = "gateway_cert.der";

    // Client private key file.
    constexpr const char* CLIENT_PRIVATE_KEY = "gateway_key.pem";

    // Node identifier of the CODESYS Level variable.
    constexpr const char* LEVEL_NODE_ID = "ns=4;s=|var|CODESYS Control Win V3 x64.Application.GVL.Level";
}