#ifndef TIMEUTILS_H
#define TIMEUTILS_H

#include <chrono>

/**
 * @brief Returns the current system time in milliseconds since the Unix epoch.
 * 
 * Used by connectors as the Sparkplug timestamp when the data source itself
 * cannot provide a reliable epoch-based timestamp...
 */
inline unsigned long long nowMillis()
{
    using namespace std::chrono;

    return static_cast<unsigned long long>(
        duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count()
    );
}


#endif