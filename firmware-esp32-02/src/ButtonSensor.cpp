#include "ButtonSensor.h"

ButtonSensor::ButtonSensor(uint8_t pin) : pin_(pin)
{

}

bool ButtonSensor::initialize()
{
    pinMode(pin_, INPUT_PULLUP);
    return true;
}

bool ButtonSensor::read(SensorReading& reading)
{
    int val = digitalRead(pin_);

    reading.type = SensorType::BUTTON;
    reading.data.button.pressed = (val == LOW);
    reading.data.button.timestamp = millis();

    return true;

}

const char* ButtonSensor::name() const{
    return "ButtonSensor";
}