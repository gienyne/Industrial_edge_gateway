package com.gienyne.industrialedgegateway.persistence.metric;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;


import com.gienyne.industrialedgegateway.persistence.domain.DeviceMetric;
import com.gienyne.industrialedgegateway.persistence.domain.Measurement;
import com.gienyne.industrialedgegateway.persistence.metric.models.api.ApiHistoryPoint;
import com.gienyne.industrialedgegateway.persistence.metric.models.api.ApiHistoryStatistics;
import com.gienyne.industrialedgegateway.persistence.metric.models.api.ApiMetricHistoryResponse;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceMetricRepository;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceRepository;
import com.gienyne.industrialedgegateway.persistence.repository.MeasurementRepository;
import com.gienyne.industrialedgegateway.persistence.shared.error.ResourceNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@Transactional(readOnly = true)
public class MetricHistoryService{

    private static final long TARGET_POINTS = 500;

    private static final int[] BUCKET_SIZES_SECONDS = {
        1, 5, 10, 30, 60,
        300, 600, 900, 1800,
        3600, 7200, 14400, 21600,
        43200, 86400
    };


    private final DeviceRepository deviceRepository;
    private final DeviceMetricRepository deviceMetricRepository;
    private final MeasurementRepository measurementRepository;

    public MetricHistoryService(DeviceRepository deviceRepository, DeviceMetricRepository deviceMetricRepository, MeasurementRepository measurementRepository){

        this.deviceRepository = deviceRepository;
        this.deviceMetricRepository = deviceMetricRepository;
        this.measurementRepository = measurementRepository;

    }

    // Returns the chronological history of one metric for one device
    // within an inclusive time range.
    public ApiMetricHistoryResponse getHistory(String deviceId, String metricName, Instant from, Instant to){

        validateRequest(deviceId, metricName, from, to);

            // Verify that the requested device exists.
        deviceRepository.findById(deviceId).orElseThrow(() -> new ResourceNotFoundException("Device was not found"));


            // Verify that the requested metric belongs to this device.
        DeviceMetric metric = deviceMetricRepository.findByDeviceId(deviceId).stream()
                .filter(candidate -> candidate.getMetricName().equals(metricName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Metric was not found for this device"));
            
        
        String dataType = metric.getDatatype();
        String unit = normalizeUnit(metric.getUnit());

        
        long measurementCount = measurementRepository.countByDeviceIdAndMetricNameAndTimeBetween(deviceId, metricName, from, to);

            
        return switch(dataType){

            case "Integer", "Double" -> getNumericHistory(deviceId, metricName, dataType, unit, from, to, measurementCount);
            case "Boolean", "String" -> getStateHistory(deviceId, metricName, dataType, unit, from, to, measurementCount);

            default -> throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unsupported metric data type: " + dataType);

        };

    }


    private ApiMetricHistoryResponse getNumericHistory(
        String deviceId,
        String metricName,
        String dataType,
        String unit,
        Instant from,
        Instant to,
        long measurementCount
    ){
        
        int bucketSeconds = chooseBucketSeconds(from, to);

        List<MeasurementRepository.NumericBucket> buckets = measurementRepository.aggregateNumericMeasurements(deviceId, metricName, from, to, bucketSeconds);

        List<ApiHistoryPoint> points = buckets.stream()
            .map(bucket -> new ApiHistoryPoint(Instant.ofEpochMilli(
                bucket.getBucketTimestampMillis()), 
                bucket.getAverageValue(),
                bucket.getMinimumValue(),
                bucket.getMaximumValue())
                ).toList();


        Double min = null;
        Double max = null;
        double weightedSum = 0.0;
        long aggregatedCount = 0;


        for(MeasurementRepository.NumericBucket bucket : buckets){

            Double bucketMin = bucket.getMinimumValue();
            Double bucketMax = bucket.getMaximumValue();
            Double bucketAverage = bucket.getAverageValue();
            Long bucketCount = bucket.getSampleCount();

            if(bucketMin != null){
                min = min == null ? bucketMin : Math.min(min, bucketMin);
            }

            if(bucketMax != null){
                max = max == null ? bucketMax : Math.max(max, bucketMax);
            }

            if(bucketAverage != null && bucketCount != null && bucketCount > 0){
                weightedSum += bucketAverage * bucketCount;
                aggregatedCount += bucketCount;
            }
        }

        Double average = aggregatedCount > 0 ? weightedSum / aggregatedCount : null;

        Optional<Measurement> latestMeasurement = measurementRepository.findFirstByDeviceIdAndMetricNameAndTimeBetweenOrderByTimeDesc(deviceId, metricName, from, to);

        Object latest = latestMeasurement.map(this::extractValue).orElse(null);

        ApiHistoryStatistics statistics = new ApiHistoryStatistics(min, max, average, latest);


        return new ApiMetricHistoryResponse(deviceId, metricName, dataType, unit, from, to, measurementCount, "time_bucket_average", (long)bucketSeconds, points, statistics);

    }


    private ApiMetricHistoryResponse getStateHistory(
        String deviceId,
        String metricName,
        String dataType,
        String unit,
        Instant from,
        Instant to,
        long measurementCount
    ){

        List<MeasurementRepository.StateChange> stateChanges = measurementRepository.findStateChanges(deviceId, metricName, from, to);

        List<ApiHistoryPoint> points = stateChanges.stream()
            .map(
                change -> {Instant timestamp = Instant.ofEpochMilli(change.getTimestampMillis());
                
                if(timestamp.isBefore(from)){
                    timestamp = from;
                }

                Object value = "Boolean".equals(dataType) ? Boolean.valueOf(change.getValue()) : change.getValue();

                return new ApiHistoryPoint(timestamp, value);
                
            }).toList();

        Object latest = points.isEmpty() ? null : points.get(points.size() - 1).value();

        ApiHistoryStatistics statistics = new ApiHistoryStatistics(null, null, null, latest);


        return new ApiMetricHistoryResponse(
            deviceId,
            metricName,
            dataType,
            unit,
            from,
            to,
            measurementCount,
            "state_changes",
            null,
            points,
            statistics
        );

    }


    private int chooseBucketSeconds(Instant from, Instant to){

        long durationSeconds = Math.max(1L, Duration.between(from, to).getSeconds());

        long idealBucketSeconds = Math.max(1L, (durationSeconds + TARGET_POINTS - 1) / TARGET_POINTS);

        for(int bucketSeconds : BUCKET_SIZES_SECONDS){
            
            if(bucketSeconds >= idealBucketSeconds){
                return bucketSeconds;
            }
        }

        return (int) Math.min(idealBucketSeconds, Integer.MAX_VALUE);

    }


    private Object extractValue(Measurement measurement){
        
        return switch(measurement.getDatatype()){
            
            case "Boolean" -> measurement.getValueBool();
            case "Integer" -> measurement.getValueInt();
            case "Double" -> measurement.getValueDouble();
            case "String" -> measurement.getValueString();
            default -> null;

        };

    }


    private String normalizeUnit(String unit){

        return unit == null || unit.isBlank() || unit.equals("-") ? null : unit;

    }


    private void validateRequest(String deviceId, String metricName, Instant from, Instant to){

        if(deviceId == null || deviceId.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device ID must not be blank");
        }

        if(metricName == null || metricName.isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metric name must not be blank");
        }

        if(from == null || to == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both start and end dates are required");
        }

        if(from.isAfter(to)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The start date must not be after the end date");
        }
    }

}