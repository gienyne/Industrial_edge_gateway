#ifndef DHT11SENSOR_H
#define DHT11SENSOR_H

#include "ISensor.h"
#include <DHT.h>
#define DHT11_PIN 4

class DHT11Sensor : public ISensor
{
    public:

        DHT11Sensor(uint8_t pin);
        bool initialize() override;
        bool read(SensorReading& reading) override;
        const char* name() const override;

    private:
    
        DHT dht_;
};
#endif