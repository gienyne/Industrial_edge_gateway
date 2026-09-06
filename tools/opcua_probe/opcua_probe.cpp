#include "cert_persistence.hpp"

#include "config/OpcUaConfig.hpp"
#include "config/OpcUaSecrets.hpp"

#include <open62541pp/client.hpp>
#include <open62541pp/node.hpp>

#include <cstdint>
#include <iostream>

int main(){

    try{

        // Load the cryptographic identity of the OPC UA client.
        auto identity = loadClientIdentity(OpcUaConfig::CLIENT_CERTIFICATE, OpcUaConfig::CLIENT_PRIVATE_KEY);

        /**
         * Create the OPC UA client configuration using
         * the client certificate and private key.
         */
        opcua::ClientConfig config(identity.certificate, identity.privateKey, {});

        // Enable encrypted and signed communication.
        config.setSecurityMode(opcua::MessageSecurityMode::SignAndEncrypt);

        // Set the Application URI of the OPC UA client.
        UA_String_clear(&config.handle()->clientDescription.applicationUri);

        config.handle()->clientDescription.applicationUri = UA_STRING_ALLOC (OpcUaConfig::APPLICATION_URI);

        // Configure username/password authentication.
        config.setUserIdentityToken(opcua::UserNameIdentityToken{
            OpcUaSecrets::USERNAME,
            OpcUaSecrets::PASSWORD
        });

        // Create the OPC UA client.
        opcua::Client client(std::move(config));

        // Connect to the OPC UA server.
        client.connect(OpcUaConfig::SERVER_URL);

        std::cout << "Successfully connected to the OPC UA server." << std::endl;

        // Create a node object representing the CODESYS Level variable.
        opcua::Node node (client,  opcua::NodeId::parse( OpcUaConfig::LEVEL_NODE_ID));


        // Read the current value of the node.
        auto value = node.readValue();

        // Try to read the CODESYS REAL value as a float.
        try {
            std::cout << "Level (float) = " << value.scalar<float>() << std::endl;
        }
        catch(...)
        {

            // Try double if float conversion fails.
            try{
                std::cout << "Level (double) = " << value.scalar<double>() << std::endl;
            }
            catch(...)
            {
                // Try int32 if the previous conversions fail.
                try {
                    std::cout << "Level (int32) = " << value.scalar<int32_t>() << std::endl;
                }
                catch(...)
                {
                    std::cerr << "Unable to convert the node value. " << std::endl;
                }
            }
        }

        // Close the OPC UA connection.
        client.disconnect();
    }
    catch(const std::exception& e)
    {
        std::cerr << "OPC UA error: " << e.what() << std::endl;
        return 1;
    }

    return 0;

}