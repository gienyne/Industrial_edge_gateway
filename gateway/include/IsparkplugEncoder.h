#ifndef ISPARKPLUGENCODER_H
#define ISPARKPLUGENCODER_H

#include <string>
#include "DeviceData.h"
#include "Sparkplugpayload.h"


/**
 * @brief Interface for encoding gateway data into Sparkplug B messages.
 * 
 * The encoder provides separate operations for Edge Node-level messages
 * and Device-level messages.
 */
class IsparkplugEncoder
{

    public:

        /**
         * @brief Virtual destructor for proper destruction through the interface.
         */
        virtual ~IsparkplugEncoder() = default;



        // -- Node-level (Edge Node) ------

        /**
         * @brief Encodes an NBIRTH message for the Edge Node.
         */
        virtual SparkplugPayload encodeNodeBirth() = 0;

        /**
         * @brief Encodes an NDEATH message for the Edge Node.
         */
        virtual SparkplugPayload encodeNodeDeath() = 0;


        // -- Device level ------


        /**
         * @brief Encodes a DBIRTH message for a Device.
         * 
         * @param deviceData Data describing the Device and its metrics.
         */
        virtual SparkplugPayload encodeDeviceBirth(const DeviceData& deviceData) = 0;

        /**
         * @brief Encodes a DDATA message containing Device metrics.
         */
        virtual SparkplugPayload encodeDeviceData(const DeviceData& deviceData) = 0;

        /**
         * @brief Encodes a DDEATH message for a Device.
         * 
         * @param deviceId Identifier of the Device.
         */
        virtual SparkplugPayload encodeDeviceDeath(const std::string& deviceId) = 0;
    

        /**
         * @brief Returns the MQTT topic used for Node Control commands.
         * 
         * @return Sparkplug NCMD topic for the Edge Node.
         */
        virtual std::string nodeCommandTopic() const = 0;


        /**
         * @brief Builds the Sparkplug payload used for the MQTT Last Will.
         */
        virtual SparkplugPayload buildWillPayload() = 0;


};

#endif
