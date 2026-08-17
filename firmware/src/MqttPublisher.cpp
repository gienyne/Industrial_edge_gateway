#include "MqttPublisher.h"
#include "Config.h"
#include <ArduinoJson.h>
#include "SensorConnector.h"

// Use the WiFi client for MQTT communication
MqttPublisher::MqttPublisher() : mqttClient_(wifiClient_)
{

}

// Initialize WiFi and MQTT
bool MqttPublisher::initialize()
{
    connectWiFi();
    mqttClient_.setServer(MQTT_BROKER, MQTT_PORT);
    connectMqtt();

    return true;
}


// Connect the ESP32 to the configured WiFi network
void MqttPublisher::connectWiFi()
{
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.println("Connecting to the Wifi");

    while(WiFi.status() != WL_CONNECTED){
        delay(500);
        Serial.println(".");
    }

    Serial.println();
    Serial.print("Wifi Connected: IP: ");
    Serial.println(WiFi.localIP());
}


// Connect to the configured MQTT broker
void MqttPublisher::connectMqtt()
{
    while(!mqttClient_.connected()){

        Serial.println("connecting to the broker..");

        if(mqttClient_.connect(MQTT_CLIENT_ID)){
            Serial.print("connected");
        }
        else{
            Serial.print("connection failed, rc=");
            Serial.print(mqttClient_.state());
            Serial.println("retrying in 2s");
            delay(2000);
        }
    }
}


// Restore WiFi and MQTT connections if necessary
void MqttPublisher::ensureConnection()
{
    if(WiFi.status() != WL_CONNECTED){
        connectWiFi();
    }

    if(!mqttClient_.connected()){
        connectMqtt();
    }

    mqttClient_.loop();
}


// Serialize the source data and publish it to the MQTT broker
bool MqttPublisher::publish(const SourceData& data)
{

    StaticJsonDocument<512> doc;

    doc["deviceId"] = DEVICE_ID;
    doc["timestamp"] = millis();

    JsonArray readings = doc.createNestedArray("readings");

    for(size_t i = 0; i < data.count; i++){

        const SensorReading& reading = data.readings[i];
        JsonObject read = readings.createNestedObject();

        switch(reading.type){

            case SensorType::DHT11:
                read["type"] = "DHT11";
                read["temperature"] = reading.data.dht11.temperature;
                read["humidity"] = reading.data.dht11.humidity;
                read["timestamp"] = reading.data.dht11.timestamp;
                break;

            case SensorType::SHOCK:
                read["type"] = "SHOCK";
                read["detected"] = reading.data.shock.detected;
                read["timestamp"] = reading.data.shock.timestamp;
                break;

            case SensorType::LIGHT:
                read["type"] = "LIGHT";
                read["intensity"] = reading.data.light.intensity;
                read["timestamp"] = reading.data.light.timestamp;
                break;

        }

    }

    char buffer[512];

    // Convert the JSON document into a character buffer
    size_t len = serializeJson(doc, buffer);

    // Define device topic
    String topic = String("raw/") + DEVICE_ID ;

    return mqttClient_.publish(topic.c_str(), buffer, len);
    
}