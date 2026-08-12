#ifndef SHOCKSENSOR_H
#define SHOCKSENSOR_H

#include <Arduino.h>
#include "ISensor.h"
#define SHOCKSENSOR_PIN 26

/**
 * @brief Sensor implementation for shock detection.
 */
class ShockSensor : public ISensor
{
    public:
        
        ShockSensor(uint8_t pin);
        bool initialize() override;
        bool read(SensorReading& reading) override;
        const char* name() const override;

    private: 
          
        // GPIO pin connected to the shock sensor
        uint8_t pin_;

};
#endif