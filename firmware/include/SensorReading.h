#ifndef SENSORREADING_H
#define SENSORREADING_H

/**
 * @brief Identifies the type of sensor that produced a reading.
 */
enum class SensorType
{
    DHT11,
    SHOCK,
    LIGHT
};

/**
 * @brief Measurement data produced by a DHT11 sensor.
 */
struct DHT11Reading
{
    float temperature;
    float humidity;
    unsigned long timestamp;
};


/**
 * @brief Measurement data produced by a shock sensor.
 */
struct ShockReading
{
    bool detected;
    unsigned long timestamp;
};


/**
 * @brief Measurement data produced by a light sensor.
 */
struct LightReading
{
    int intensity;
    unsigned long timestamp;
};

/**
 * @brief Stores the measurement data of different sensor types.
 * Only the member matching the SensorType is used.
 */
union SensorReadingData
{
    DHT11Reading dht11;
    ShockReading shock;
    LightReading light;
};


/**
 * @brief Source-specific sensor reading.
 * Contains the sensor type and its corresponding measurement data.
 */
struct SensorReading
{
    SensorType type;
    SensorReadingData data;
};

#endif