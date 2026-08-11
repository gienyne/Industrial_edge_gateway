#ifndef SENSORREADING_H
#define SENSORREADING_H

enum class SensorType
{
    DHT11,
    SHOCK,
    LIGHT
};

struct DHT11Reading
{
    float temperature;
    float humidity;
    unsigned long timestamp;
};

struct ShockReading
{
    bool detected;
    unsigned long timestamp;
};

struct LightReading
{
    int intensity;
    unsigned long timestamp;
};

union SensorReadingData
{
    DHT11Reading dht11;
    ShockReading shock;
    LightReading light;
};

struct SensorReading
{
    SensorType type;
    SensorReadingData data;
};

#endif