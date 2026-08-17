#include "ESP32Connector.h"
#include <nlohmann/json.hpp>
#include <iostream>

using json = nlohmann::json;

ESP32Connector::ESP32Connector(const ESP32ConnectorConfig& config) : config_(config),
mqttClient_(config.brokerAddress, config.deviceId)
{

} 

bool ESP32Connector::initialize()
{
    mqttClient_.set_callback(*this);

    mqtt::connect_options connOpts;
    connOpts.set_clean_session(true);

    try{
        mqttClient_.connect(connOpts)->wait();
        mqttClient_.subscribe(config_.topicFilter, 0)->wait();
        return true;
    }
    catch (const mqtt::exception& exc)
    {
        std::cerr <<"ESP32Connector: MQTT error" << exc.what() << std::endl;
        return false;
    }
}

std::string ESP32Connector::extractDeviceId(const std::string& topic) const{
    //topic format: raw/<deviceID>
    const std::string prefix = "raw/";

    if(topic.rfind(prefix, 0) == 0){
        return topic.substr(prefix.size());
    }

    return topic;
}


void ESP32Connector::message_arrived(mqtt::const_message_ptr msg){

    std::lock_guard<std::mutex> lock(mutex_);
    std::string deviceId = extractDeviceId(msg->get_topic());
    pendingPayloads_[deviceId] = msg->get_payload_str();

}

void ESP32Connector::connection_lost(const std::string& lst){
    std::cerr << "ESP32Connector: connection lost: " << lst << std::endl;
}

std::vector<DeviceData> ESP32Connector::collectData()
{
    std::map<std::string, std::string> payloadsToProcess;

    std::lock_guard<std::mutex> lock(mutex_);

    {

        if(pendingPayloads_.empty()){
        return {};
    }

        payloadsToProcess.swap(pendingPayloads_);

    }

    std::vector<DeviceData> result;
    result.reserve(payloadsToProcess.size());

    for(const auto& [deviceId, payload] : payloadsToProcess){
        result.push_back(parsePayload(deviceId, payload));
    }

    return result;
}

DeviceData ESP32Connector::parsePayload(const std::string& deviceId, const std::string& payload)
{
    DeviceData data;

    json doc = json::parse(payload);

    data.deviceId = doc.value("deviceId", config_.deviceId);
    data.timestamp = doc.value("timestamp", 0ULL);

    for(const auto& reading : doc["readings"]){
        std::string type = reading.value("type", "");

        if(type == "DHT11"){
            data.metrics.push_back(Metric{
                "temperature", MetricDataType::Float,
                reading.value("temperature", 0.0), "C",
                reading.value("timestamp", 0ULL)
            });

            data.metrics.push_back(Metric{
                "humidity", MetricDataType::Float,
                reading.value("humidity", 0.0), "%",
                reading.value("timestamp", 0ULL)
            });
        }

        else if(type == "SHOCK"){
            data.metrics.push_back(Metric{
                "shockDetected", MetricDataType::Boolean,
                reading.value("detected", false), "",
                reading.value("timestamp", 0ULL)
            });
        }

        else if(type == "LIGHT")
        {
            data.metrics.push_back(Metric{
                "lightIntensity", MetricDataType::Integer,
                reading.value("intensity", 0), "",
                reading.value("timestamp", 0ULL)
            });
        }
    }

    return data;
}

const char* ESP32Connector::name() const {
    return "ESP32Connector";
}