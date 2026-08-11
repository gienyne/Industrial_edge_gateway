#ifndef SHOCKSENSOR_H
#define SHOCKSENSOR_H

#include <Arduino.h>
#include "ISensor.h"
#define SHOCKSENSOR_PIN 26

class ShockSensor : public ISensor
{
    public:
        
        ShockSensor(uint8_t pin);
        bool initialize() override;
        bool read(SensorReading& reading) override;
        const char* name() const override;

    private: 
          
        uint8_t pin_;

};
#endif