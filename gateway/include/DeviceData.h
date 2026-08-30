#ifndef DEVICEDATA_H
#define DEVICEDATA_H

#include <string>
#include <vector>
#include "Metric.h"

/**
 * @brief Represents the data collected from a single device
 */
struct DeviceData{
    std::string deviceId;
    std::vector<Metric> metrics;
    unsigned long long timestamp;
};

#endif