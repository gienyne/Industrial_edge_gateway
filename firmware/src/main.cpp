#include <Arduino.h>
#include "DHT11Sensor.h"
#include "ShockSensor.h"
#include "LightSensor.h"
#include "SensorConnector.h"

DHT11Sensor dhtSensor(DHT11_PIN);
ShockSensor shockSensor(SHOCKSENSOR_PIN);
LightSensor lightSensor(LIGHT_PIN);

SensorArray sensors = {&dhtSensor, &shockSensor, &lightSensor};
SensorConnector connector(sensors);

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

}

void loop() {

  SourceData data = connector.collectData();

  Serial.print("collected ");
  Serial.println(data.count);

  Serial.println("reading:");

  for(size_t i = 0; i < data.count; i++){

    SensorReading& reading = data.readings[i];

    switch(reading.type){

      case SensorType::DHT11:

        Serial.print("  DHT11 -> temperature: ");
        Serial.print(reading.data.dht11.temperature);
        Serial.print(" °C, humidity: ");
        Serial.print(reading.data.dht11.humidity);
        Serial.println(" %");
        break;

      case SensorType::SHOCK:
        
        Serial.print("  Shock -> detected: ");
        Serial.println(reading.data.shock.detected ? "YES" : "NO");
        break;

      case SensorType::LIGHT:

        Serial.print("  Light -> intensity (raw): ");
        Serial.println(reading.data.light.intensity);
        break;
    }
  }

  delay(2000);

}
