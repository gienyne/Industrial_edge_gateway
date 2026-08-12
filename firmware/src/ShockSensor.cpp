#include "ShockSensor.h"

ShockSensor::ShockSensor(uint8_t pin) : pin_(pin)
{

}

bool ShockSensor::initialize()
{
    // Configure the shock sensor pin with an internal pull-up resistor.
    pinMode(pin_, INPUT_PULLUP);
    return true;
}

bool ShockSensor::read(SensorReading& reading)
{
    int val = digitalRead(pin_);

    reading.type = SensorType::SHOCK;
    reading.data.shock.detected = (val == LOW);
    reading.data.shock.timestamp = millis();

    return true;
}

// Return the sensor name.
const char* ShockSensor::name() const
{
  return "ShockSensor";
}