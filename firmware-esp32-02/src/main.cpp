#include <Arduino.h>
#include "DHT11Sensor.h"
#include "ButtonSensor.h"
#include "SensorConnector.h"
#include "MqttPublisher.h"

DHT11Sensor dhtSensor(DHT11_PIN);
ButtonSensor buttonSensor(BUTTONSENSOR_PIN);

SensorArray sensors = {&dhtSensor, &buttonSensor};
SensorConnector connector(sensors);

MqttPublisher mqttPublisher;

void setup() {

  Serial.begin(115200);
  delay(1000);

  Serial.println("industrial edge gateway");
  Serial.println("ESP32 is running");

  if(connector.initialize()){
    Serial.println("SensorConnector initialized");
  }
  else {
     Serial.println("SensorConnector initialization failed");
  }

  if(mqttPublisher.initialize()){
    Serial.println("MqttPublisher initialized");
  }

  else {
     Serial.println("MqttPublisher initialization failed");
  }

}

void loop() {

  mqttPublisher.ensureConnection();
  SourceData data = connector.collectData();

  Serial.print("collected ");
  Serial.print(data.count);
  Serial.println(" readings");

  if(mqttPublisher.publish(data)){
    Serial.println("published to MQTT broker");
  }
  else{
    Serial.println("MQTT publish failed");
  }

  delay(2000);
  
}