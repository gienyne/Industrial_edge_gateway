#include <Arduino.h>
#include "DHT11Sensor.h"
#include "ShockSensor.h"
#include "LightSensor.h"

DHT11Sensor dhtSensor(DHT11_PIN);
ShockSensor shockSensor(SHOCKSENSOR_PIN);
LightSensor lightSensor(LIGHT_PIN);

void setup() {

  Serial.begin(115200);
  delay(1000);

  Serial.println("industrial edge gateway");
  Serial.println("ESP32 is running");

  if(dhtSensor.initialize()){
    Serial.println("DHT11Sensor initialized");
  }
  else {
     Serial.println("DHT11Sensor initialization failed");
  }

  if(shockSensor.initialize()){
    Serial.println("ShockSensor initialized");
  }
  else{
    Serial.println("ShockSensor initialization failed");
  }

  if(lightSensor.initialize()){
    Serial.println("LightSensor initialized");
  }
  else{
    Serial.println("LightSensor initialization failed");
  }
  
}

void loop() {

  SensorReading dhtReading;
  SensorReading shockReading;
  SensorReading lightReading;

  if(dhtSensor.read(dhtReading)){
    Serial.print("temperature: ");
    Serial.print(dhtReading.data.dht11.temperature);
    Serial.println(" °C");

    Serial.print("humidity: ");
    Serial.print(dhtReading.data.dht11.humidity);
    Serial.println(" %");
  }
  else{
    Serial.println("DHT11 read failed");
  }

  if(shockSensor.read(shockReading)){
    Serial.print("shock detected: ");
    Serial.println(shockReading.data.shock.detected ? "YES" : "NO");
  }

  if(lightSensor.read(lightReading)){
    Serial.print("light intensity (raw): ");
    Serial.println(lightReading.data.light.intensity);
  }

  delay(2000);

}
