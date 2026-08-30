#ifndef DHT11SENSOR_H
#define DHT11SENSOR_H

#include "ISensor.h"
#include <DHT.h>

// GPIO pin used by the DHT11 sensor
#define DHT11_PIN 4

/**
 * @brief Sensor implementation for the DHT11.
 * Reads temperature and humidity from a DHT11 sensor.
 */
class DHT11Sensor : public ISensor
{
    public:

        DHT11Sensor(uint8_t pin);
        bool initialize() override;
        bool read(SensorReading& reading) override;
        const char* name() const override;

    private:
    
        // DHT library object used to communicate with the sensor
        DHT dht_;
};
#endif