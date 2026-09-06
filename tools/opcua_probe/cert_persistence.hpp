#pragma once

#include <open62541pp/types.hpp>

#include <filesystem>
#include <fstream>
#include <iterator>
#include <stdexcept>
#include <vector>


// Stores the cryptographic identity of the OPC UA client.
struct ClientIdentity
{
    opcua::ByteString certificate;
    opcua::ByteString privateKey;
};



// Load a binary file and return its content as an OPC UA ByteString.
inline opcua::ByteString loadBinaryFile(const std::filesystem::path& path)
{
    std::ifstream file(path, std::ios::binary);

    if(!file){
        throw std::runtime_error("Unable to open file: " + path.string());
    }

    std::vector<uint8_t> buffer((std::istreambuf_iterator<char>(file)), std::istreambuf_iterator<char>());

    return opcua::ByteString(buffer.begin(), buffer.end());

}

// Load the client certificate and private key from disk.
inline ClientIdentity loadClientIdentity(const std::filesystem::path& certPath, const std::filesystem::path& keyPath)
{
    if(!std::filesystem::exists(certPath) || !std::filesystem::exists(keyPath)){

        throw std::runtime_error("Client certificate or private key not found.");

    }

    return ClientIdentity {
        loadBinaryFile(certPath),
        loadBinaryFile(keyPath)
    };
}