#include "DHT11Sensor.h"

DHT11Sensor::DHT11Sensor(uint8_t pin) : dht_(pin, DHT11)
{

}

bool DHT11Sensor::initialize()
{
    dht_.begin();
    return true;
}

bool DHT11Sensor::read(SensorReading& reading)
{
    float temperature = dht_.readTemperature();
    float humidity = dht_.readHumidity();

    if(isnan(temperature) || isnan(humidity)){
        reading.type = SensorType::DHT11;
        return false;
    }

    reading.type = SensorType::DHT11;
    reading.data.dht11.temperature = temperature;
    reading.data.dht11.humidity = humidity;
    reading.data.dht11.timestamp = millis();

    return true;
}

const char* DHT11Sensor::name() const {
    return "DHT11Sensor";
}