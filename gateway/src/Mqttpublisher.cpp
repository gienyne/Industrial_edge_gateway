#include "Mqttpublisher.h"
#include "../build/proto/sparkplug_b.pb.h"
#include <iostream>

namespace
{
    constexpr const char* REBIRTH_METRIC_NAME = "Node Control/Rebirth";
}


Mqttpublisher::Mqttpublisher(const MQTTPublisherConfig& config, SparkplugEncoder& encoder) : config_(config), encoder_(encoder), mqttClient_(config.brokerAddress, config.clientId), bdSeqManager_(config.bdSeqFilePath)
{

}

bool Mqttpublisher::initialize()
{

    mqttClient_.set_callback(*this);
   
    
    encoder_.setBdSeq(bdSeqManager_.nextSessionBdSeq());

    /**
     * The NDEATH message is used as the MQTT Last Will.
     * 
     * If the gateway loses its MQTT connection unexpectedly,
     * the broker publishes this message automatically.
     */
    SparkplugPayload will = encoder_.buildWillPayload();

    mqtt::message_ptr willMsg = mqtt::message::create(
        will.topic,
        will.payload.data(),
        will.payload.size(),
        will.qos,
        will.retain
    );

    mqtt::connect_options connOpts;

    connOpts.set_clean_session(true);

    connOpts.set_will_message(willMsg);

    try{

        mqttClient_.connect(connOpts)->wait();

        mqttClient_.subscribe(encoder_.nodeCommandTopic(), /*qos=*/1)->wait();

        return true;
    }
    catch(const mqtt::exception& exc)
    {
        std::cerr << "MQTTPublisher: connection error" << exc.what() << std::endl;
        return false;
    }
}

bool Mqttpublisher::publish(const SparkplugPayload& payload)
{
    try {

        /**
         * The payload is already completely prepared by the SparkplugEncoder. 
         * The publisher therefore does not need to interpret or modify the Sparkplug message.
         */
        mqttClient_.publish(payload.topic, payload.payload.data(), payload.payload.size(), payload.qos, payload.retain)->wait();
        return true;

    }
    catch (const mqtt::exception& exc){

        std::cerr << "MQTTPublisher: publish error " << exc.what() << std::endl;
        return false;

    }
}

bool Mqttpublisher::disconnect()
{
    try{

        /**
         * A clean MQTT disconnect prevents the broker from 
         * publishing the configured Last Will message.
         */
        mqttClient_.disconnect()->wait();
        return true;
    }
    catch(const mqtt::exception& exc)
    {
        std::cerr << "MQTTPublisher: disconnect error " << exc.what() << std::endl;
        return false;
    }
}

/**
 * @brief Checks for a pending rebirth request and consumes it.
 * 
 * @return true if a rebirth request was pending, false otherwise
 */
bool Mqttpublisher::consumeRebirthRequest()
{
    return rebirthRequested_.exchange(false);
}


/**
 * @brief Handles an incoming MQTT message.
 * 
 * Decodes the received Sparkplug payload and checks for a Node Control/Rebirth command.
 * 
 * @param msg MQTT message received by the client.
 */
void Mqttpublisher::message_arrived(mqtt::const_message_ptr msg)
{
    org::eclipse::tahu::protobuf::Payload payload;

    const std::string& raw = msg->get_payload();

    if(!payload.ParseFromArray(raw.data(), static_cast<int>(raw.size()))){

        std::cerr << "MQTTPublisher: failed to decode incoming NCMD payload" << std::endl;
        return;
    }

    for(const auto& metric : payload.metrics())
    {
        if(metric.name() == REBIRTH_METRIC_NAME && metric.has_boolean_value() && metric.boolean_value()){
            rebirthRequested_.store(true);
            return;
        }
    }
}


/**
 * @brief Handles the loss of the MQTT connection.
 * 
 * @param lst Description of the connection loss.
 */
void Mqttpublisher::connection_lost(const std::string& lst){
    std::cerr << "MQTTPublisher: connection lost: " << lst << std::endl;
}