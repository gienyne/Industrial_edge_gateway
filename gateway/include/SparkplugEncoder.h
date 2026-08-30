#ifndef SPARKPLUGENCODER_H
#define SPARKPLUGENCODER_H

#include <cstdint>
#include <string>
#include "Isparkplugencoder.h"
#include "../build/proto/sparkplug_b.pb.h"

/**
 * @brief Configuration required by the Sparkplug encoder.
 * 
 * Defines the identifiers used to construct Sparkplug B topic names.
 */
struct SparkplugEncodeConfig
{
    std::string namespaceId;
    std::string groupId;
    std::string edgeNodeId;
};


/**
 * @brief Encodes gateway data into Sparkplug B messages.
 * 
 * The encoder is responsible for the Sparkplug-specific representation of
 * Node-level and Device-level messages. It builds MQTT topics, populates
 * the Sparkplug B protobuf payload and manages Sparkplug sequence numbers.
 */
class SparkplugEncoder : public IsparkplugEncoder
{

    public:

        /**
         * @brief Constructs a Sparkplug encoder with the given configuration.
         * 
         * @param config Sparkplug namespace, group and Edge Node identifiers.
         */
        explicit SparkplugEncoder(const SparkplugEncodeConfig& config);


        // -- Node-level (Edge Node) --------

        /**
         * @brief Encodes an NBIRTH message for the Edge Node.
         * 
         * @return SparkplugPayload containing the NBIRTH topic and payload.
         */
        SparkplugPayload encodeNodeBirth() override;

        /**
         * @brief Encodes an NDEATH message for the Edge Node.
         * 
         * @return SparkplugPayload containing the NDEATH topic and payload.
         */
        SparkplugPayload encodeNodeDeath() override;


        // -- Device-level -------------

        /**
         * @brief Encodes a DBIRTH message for a device.
         * 
         * @param deviceData Device identity and metrics to be included in the device birth message.
         * @return SparkplugPayload containing the DBIRTH topic and payload.
         */
        SparkplugPayload encodeDeviceBirth(const DeviceData& deviceData) override;

        /**
         * @brief Encodes a DDATA message containing current device metrics.
         * 
         * @param deviceData Device identity and metrics.
         * @return SparkplugPayload containing the DDATA topic and payload.
         */
        SparkplugPayload encodeDeviceData(const DeviceData& deviceData) override;

        /**
         * @brief Encodes a DDEATH message for a device.
         * 
         * @param deviceId Identifier of the device that has gone offline.
         * @return SparkplugPayload containing the DDEATH topic and payload.
         */
        SparkplugPayload encodeDeviceDeath(const std::string& deviceId) override;

        /**
         * @brief Builds the MQTT Last Will payload for the Edge Node.
         * 
         * The returned payload represents the NDEATH message that can be
         * configured as the MQTT Last Will by the MQTT publisher.
         * 
         * @return SparkplugPayload containing the NDEATH topic and payload.
         */
        SparkplugPayload buildWillPayload() override;

        
        /**
         * @brief Returns the MQTT topic used to receive Node Control commands.
         * 
         * @return Sparkplug NCMD topic for this Edge Node.
         */
        std::string nodeCommandTopic() const override;


        void setBdSeq(std::uint64_t bdSeq);

    private:

        SparkplugEncodeConfig config_;
        
        /**
         * @brief Birth/death sequence number identifying the current Edge Node session.
         */
        std::uint64_t bdSeq_;

        /**
         * @brief Sequence number used for Sparkplug messages.
         * 
         * The sequence number is advanced after each encoded message and
         * wraps from 255 back to 0.
         */
        std::uint8_t seq_;

        /**
         * @brief Builds a topic for an Edge Node-level message.
         * 
         * @param message_type Sparkplug message type such as NBIRTH or NDEATH.
         * @return Sparkplug MQTT topic.
         */
        std::string buildNodeTopic(const std::string& message_type) const;

        /**
         * @brief Builds a topic for a Device-level message.
         * 
         * @param message_type Sparkplug message type such as DBIRTH, DDATA or DDEATH.
         * @param deviceId Identifier of the target device.
         * @return Sparkplug MQTT topic.
         */
        std::string buildDeviceTopic(const std::string& message_type, const std::string& deviceId) const;

        /**
         * @brief Adds a device metric to a Sparkplug protobuf payload.
         * 
         * @param payload Sparkplug protobuf payload to modify.
         * @param metric Internal metric to encode.
         * @param includeDatatype Whether the Sparkplug datatype field should be included.
         */
        void appendMetric(org::eclipse::tahu::protobuf::Payload& payload, const Metric& metric, bool includeDatatype) const;

        /**
         * @brief Adds the Sparkplug bdSeq metric to a payload.
         * 
         * @param payload Sparkplug protobuf payload to modify.
         * 
         */
        void appendBdSeqMetric(org::eclipse::tahu::protobuf::Payload& payload) const;

        /**
         * @brief Adds the Sparkplug Node Control/Rebirth metric to a payload.
         * 
         * @param payload Sparkplug protobuf payload to modify.
         */
        void appendRebirthMetric(org::eclipse::tahu::protobuf::Payload& payload) const;

        /**
         * @brief Converts an internal metric datatype to its Sparkplug datatype identifier
         * 
         * @param type Internal metric datatype.
         * @return Sparkplug datatype identifier.
         */
        std::uint32_t toSparkplugDataType(MetricDataType type) const;

        /**
         * @brief Assigns the current sequence number to a payload and advances it.
         * 
         * The sequence number wraps from 255 back to 0.
         * 
         * @param payload Sparkplug protobuf payload to update.
         */
        void assignAndAdvanceSeq(org::eclipse::tahu::protobuf::Payload& payload);

        /**
         * @brief Serializes a Sparkplug protobuf payload into a transport-ready payload.
         * 
         * @param payload Sparkplug protobuf message to serialize
         * @param topic MQTT topic associated with the message.
         * @param qos MQTT Quality of Service level.
         * @param retain MQTT retain flag.
         * 
         * @return SparkplugPayload containing topic, binary payload and MQTT properties.
         */
        SparkplugPayload serialize(const org::eclipse::tahu::protobuf::Payload& payload, const std::string& topic, int qos, bool retain) const;

        /**
         * @brief Returns the current UTC time in milliseconds.
         * 
         * @return Current Unix timestamp in milliseconds.
         */
        std::uint64_t nowMillisUtc() const;

};

#endif