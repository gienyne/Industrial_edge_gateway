package com.gienyne.industrialedgegateway.persistence.device.models.api;

import com.gienyne.industrialedgegateway.persistence.domain.Measurement;

import java.time.Instant;


// Represents one measurement returned by the API.
public record ApiMeasurementResponse(Instant timestamp, String metricName, String dataType, Object value, String unit){

    // Converts a database measurement into an API response.
    public static ApiMeasurementResponse from(Measurement measurement){

        // Reads the value from the field that matches its data type.
        Object value = switch(measurement.getDatatype()){

            case "Boolean" -> measurement.getValueBool();
            case "Integer" -> measurement.getValueInt();
            case "Double"  -> measurement.getValueDouble();
            case "String"  -> measurement.getValueString();
            default -> null;
        };

        return new ApiMeasurementResponse(measurement.getTime(), measurement.getMetricName(), measurement.getDatatype(), value, normalizeUnit(measurement.getUnit()));

    }


    private static String normalizeUnit(String unit){

        return (unit == null || unit.isBlank() || unit.equals("-")) ? null : unit;
    }
}