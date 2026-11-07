#ifndef METRIC_H
#define METRIC_H

#include <cstdint>
#include <string>
#include <variant>

/**
 * @brief Supported data types for a metric.
 */
enum class MetricDataType
{
    Boolean,
    Integer,
    UInt32,
    Double,
    String
};

/**
 * @brief Represents a single measurement.
 */
struct Metric {

    std::string name;
    MetricDataType datatype;

    // Stores the value of the metric.
    std::variant<bool, int, std::uint32_t, double, std::string> value;

    std::string unit;
    unsigned long long timestamp;
};
#endif
