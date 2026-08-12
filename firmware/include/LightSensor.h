#ifndef LIGHTSENSOR_H
#define LIGHTSENSOR_H

#include <Arduino.h>
#include "ISensor.h"
#define LIGHT_PIN 34

/**
 * @brief Sensor implementation for ambient light measurement.
 */
class LightSensor : public ISensor
{
    public:
        
        LightSensor(uint8_t pin);
        bool initialize() override;
        bool read(SensorReading& reading) override;
        const char* name() const override;

    private:
             
        uint8_t pin_;
};
#endif