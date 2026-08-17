#ifndef METRIC_H
#define METRIC_H

#include <string>
#include <variant>

/**
 * @brief Supported data types for a metric.
 */
enum class MetricDataType
{
    Boolean,
    Integer,
    Float,
    String
};

/**
 * @brief Represents a single measurement.
 */
struct Metric {

    std::string name;
    MetricDataType datatype;

    // Stores the value of the metric.
    std::variant<bool, int, double, std::string> value;

    std::string unit;
    unsigned long long timestamp;
};
#endif