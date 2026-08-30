#ifndef BUTTONSENSOR_H
#define BUTTONSENSOR_H

#include <Arduino.h>
#include "ISensor.h"

#define BUTTONSENSOR_PIN 25


class ButtonSensor : public ISensor
{
    public:

          ButtonSensor(uint8_t pin);
          bool initialize() override;
          bool read(SensorReading& reading) override;
          const char* name() const override;

    private:

         uint8_t pin_;

};

#endif