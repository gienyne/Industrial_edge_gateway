#ifndef TIMEUTILS_H
#define TIMEUTILS_H

#include <chrono>

/**
 * @brief Returns the current system time in milliseconds since the Unix epoch.
 * 
 * @return Current timestamp in milliseconds.
 */
inline unsigned long long nowMillis()
{
    using namespace std::chrono;

    return static_cast<unsigned long long>(
        duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count()
    );
}


#endif