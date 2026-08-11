#include <Arduino.h>
#include "DHT11Sensor.h"

DHT11Sensor dhtSensor(DHT11_PIN);

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

  
}

void loop() {

  SensorReading reading;

  if(dhtSensor.read(reading)){
    Serial.print("temperature: ");
    Serial.print(reading.data.dht11.temperature);
    Serial.println(" °C");

    Serial.print("humidity: ");
    Serial.print(reading.data.dht11.humidity);
    Serial.println(" %");
  }
  else{
    Serial.println("DHT11 read failed");
  }

  delay(2000);

}
