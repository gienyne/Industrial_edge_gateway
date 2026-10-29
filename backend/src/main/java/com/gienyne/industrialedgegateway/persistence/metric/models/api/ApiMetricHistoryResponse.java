package com.gienyne.industrialedgegateway.persistence.metric.models.api;


import java.time.Instant;
import java.util.List;

public record ApiMetricHistoryResponse(
    String deviceId, 
    String metricName, 
    String dataType, 
    String unit, 
    Instant from,
    Instant to,
    long measurementCount,
    String aggregation,
    Long bucketSeconds,
    List<ApiHistoryPoint> points,
    ApiHistoryStatistics statistics
){

}