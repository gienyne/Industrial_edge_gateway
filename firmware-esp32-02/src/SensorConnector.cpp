#include "SensorConnector.h"
#include <Arduino.h>

SensorConnector::SensorConnector(const SensorArray& sensors) : sensor_(sensors)
{

}

bool SensorConnector::initialize()
{
    bool allSensorOK = true;

    // Initialize all configured sensors.
    for(ISensor* sensor: sensor_){
        if(!sensor->initialize()){
            logDiagnostic(sensor->name());
            allSensorOK = false;
        }
    }

    return allSensorOK;
}

SourceData SensorConnector::collectData()
{
   SourceData data;
   data.count = 0;

   // Read data from all configured sensors.
   for(ISensor* sensor : sensor_){
    SensorReading reading;
    if(sensor->read(reading)){
        data.readings[data.count] = reading;
        data.count++;
    }

    else{
        logDiagnostic(sensor->name());
    }
   }

   return data;
}

// Return the connector name.
const char* SensorConnector::name() const{
    return "SensorConnector";
}

void SensorConnector::logDiagnostic(const char* message){
    Serial.print("SensorConnector: diagnose.....");
    Serial.println(message);
}
