#ifndef ISENSOR_H
#define ISENSOR_H

#include "SensorReading.h"

/**
 * @brief Common interface for all sensors.
 * Defines the basic operations that every sensor must provide.
 */
class ISensor {

    public:

        /**
         * @brief Destroy the sensor interface.
         */
        virtual ~ISensor() = default;

        /**
         * @brief Initialize the sensor hardware.
         * @return true if initialization was successful
         * @return false if initialization failed.
         */
        virtual bool initialize() = 0;

        /**
         * @brief Read a measurement from the sensor
         * @param reading SensorReading object that receives the measurement.
         * @return true if the reading was successful.
         * @return false if the reading failed.
         */
        virtual bool read(SensorReading& reading) = 0;

        // Return the sensor name
        virtual const char* name() const = 0;

};

#endif