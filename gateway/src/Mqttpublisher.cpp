#include "Mqttpublisher.h"
#include <iostream>

Mqttpublisher::Mqttpublisher(const MQTTPublisherConfig& config, SparkplugEncoder& encoder) : config_(config), encoder_(encoder), mqttClient_(config.brokerAddress, config.clientId)
{

}

bool Mqttpublisher::initialize()
{
    /**
     * A new Sparkplug session starts with a new bdSeq value.
     */
    encoder_.onNewSession();

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