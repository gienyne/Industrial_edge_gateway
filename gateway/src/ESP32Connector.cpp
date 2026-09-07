#include "ESP32Connector.h"
#include "TimeUtils.h"
#include <nlohmann/json.hpp>
#include <iostream>

using json = nlohmann::json;

ESP32Connector::ESP32Connector(const ESP32ConnectorConfig& config) : config_(config),
mqttClient_(config.brokerAddress, config.deviceId)
{

} 

bool ESP32Connector::initialize()
{
    // Register this connector as the callback handler for incoming MQTT messages.
    // Paho MQTT calls message_arrived() from its internal MQTT thread.
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

    // message_arrived() runs on the MQTT callback thread.
    //  Protect the shared buffer because collectData() accesses it from the Gateway's main thread.
    std::lock_guard<std::mutex> lock(mutex_);

    std::string deviceId = extractDeviceId(msg->get_topic());

    // Keep only the most recent payload for each device.
    pendingPayloads_[deviceId] = msg->get_payload_str();

}

void ESP32Connector::connection_lost(const std::string& lst){
    std::cerr << "ESP32Connector: connection lost: " << lst << std::endl;
}

std::vector<DeviceData> ESP32Connector::collectData()
{
    std::map<std::string, std::string> payloadsToProcess;

    {

        std::lock_guard<std::mutex> lock(mutex_);

        if(pendingPayloads_.empty()){
        return {};
    }

        // Move pending messages out of the shared buffer.
        // New MQTT messages can now be received immediately.
        payloadsToProcess.swap(pendingPayloads_);

    }

    std::vector<DeviceData> result;
    result.reserve(payloadsToProcess.size());

    for(const auto& [deviceId, payload] : payloadsToProcess){

        try{
            result.push_back(parsePayload(deviceId, payload));
        }
        catch(const json::exception& exc)
        {
            /**
             * A malformed JSON payload must not terminate the Gateway.
             * 
             * The MQTT message was successfully received at transport
             * level, but no usable application data could be produced.
             * 
             * keep the device information even though no  usable metrics can be extracted
             * This allows the GatewayApplication to know that the device is still
             * communicating and update its liveness timestamp.
             */
            std::cerr << "ESP32Connector: invalid JSON from device '" << deviceId << "': " << exc.what() << std::endl;

            DeviceData transportLivenessOnly{deviceId, {}, nowMillis()};
            result.push_back(transportLivenessOnly);
        }
        
    }

    return result;
}


DeviceData ESP32Connector::parsePayload(const std::string& deviceId, const std::string& payload)
{
    DeviceData data;
    data.deviceId = deviceId;

    // json::parse() may throw json::exception when the payload is malformed.
    // The exception is intentionally handled by collectData(), which decides
    // how a malformed message should affect the Gateway.
    json doc = json::parse(payload);


    // Keep the ESP32 uptime as a diagnostic metric.
    // A sudden decrease in this value indicates that the device likely rebooted.
    // millis() represents elapsed time since the ESP32 booted.
    unsigned long long uptimeMillis = doc.value("timestamp", 0ULL);
    data.metrics.push_back(Metric{
        "uptimeMs", MetricDataType::Integer, static_cast<int>(uptimeMillis), "ms", nowMillis()
    });


    // The ESP32 timestamp represents time since boot rather than Unix epoch time.
    // The Gateway therefore assigns its own current wall-clock timestamp to DeviceData.
    data.timestamp = nowMillis();

    /**
     * A syntactically valid JSON payload may still have an unexpected structure.
     * In that case no exception is raised here; metrics simply remain empty 
     * The Gateway can still use the received message as a liveness indication,
     * but it currently does not update the device's known application state (lastKnownState_).
     */
    if(doc.contains("readings") && doc["readings"].is_array()){

        for(const auto& reading : doc["readings"]){
            std::string type = reading.value("type", "");

            if(type == "DHT11"){
            data.metrics.push_back(Metric{
                "temperature", MetricDataType::Double,
                reading.value("temperature", 0.0), "C",
                nowMillis()
            });

            data.metrics.push_back(Metric{
                "humidity", MetricDataType::Double,
                reading.value("humidity", 0.0), "%",
                nowMillis()
            });
        }

        else if(type == "SHOCK"){
            data.metrics.push_back(Metric{
                "shockDetected", MetricDataType::Boolean,
                reading.value("detected", false), "",
                nowMillis()
            });
        }

        else if(type == "LIGHT")
        {
            data.metrics.push_back(Metric{
                "lightIntensity", MetricDataType::Integer,
                reading.value("intensity", 0), "",
                nowMillis()
            });
        }

        else if(type == "BUTTON")
        {
            data.metrics.push_back({
                "buttonPressed", MetricDataType::Boolean,
                reading.value("pressed", false), "",
                nowMillis()
            });
        }
    }

}

    return data;
}

const char* ESP32Connector::name() const {
    return "ESP32Connector";
}