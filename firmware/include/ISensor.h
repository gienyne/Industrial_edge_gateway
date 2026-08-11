#ifndef ISENSOR_H
#define ISENSOR_H

#include "SensorReading.h"

class ISensor {

    public:

        virtual ~ISensor() = default;
        virtual bool initialize() = 0;
        virtual bool read(SensorReading& reading) = 0;
        virtual const char* name() const = 0;

};

#endif