#ifndef SENSORCONNECTOR_H
#define SENSORCONNECTOR_H

#include <array>
#include "ISensor.h"
#include "SensorReading.h"

constexpr size_t SENSOR_COUNT = 3;

using SensorArray = std::array<ISensor*, SENSOR_COUNT>;


/**
 * @brief Contains the sensor readings collected by the connector
 */
struct SourceData
{
    std::array<SensorReading, SENSOR_COUNT> readings;
    size_t count;
};


/**
 * @brief Collects measurements from the configured sensors.
 * The connector works with sensors through the ISensor interface
 * and keeps the source-specific data on the source device.
 */
class SensorConnector
{

    public:

        /**
         * @brief Create a sensor connector using the given sensors
         * @param sensors Array of sensors managed by the connector.
         */
        SensorConnector(const SensorArray& sensors);

        /**
         * @brief Initialize all configured sensors.
         * @return true if all sensors were initialized successfully.
         * @return false if one or more sensors failed to initialize.
         */
        bool initialize();

        /**
         * @brief Collect readings from the configured sensors.
         * @return SourceData containing the valid sensor readings.
         */
        SourceData collectData();

        // Return the connector name
        const char* name() const;

    private:
         
        SensorArray sensor_;

        // Report a diagnostic message
        void logDiagnostic(const char* message);

};

#endif