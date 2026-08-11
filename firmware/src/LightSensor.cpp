#include "LightSensor.h"

LightSensor::LightSensor(uint8_t pin) : pin_(pin)
{

}

bool LightSensor::initialize()
{
    pinMode(pin_, INPUT);
    return true;
}

bool LightSensor::read(SensorReading& reading)
{
    int val = analogRead(pin_);

    reading.type = SensorType::LIGHT;
    reading.data.light.intensity = val;
    reading.data.light.timestamp = millis();

    return true;

}

const char* LightSensor::name() const
{
    return "LightSensor";
}