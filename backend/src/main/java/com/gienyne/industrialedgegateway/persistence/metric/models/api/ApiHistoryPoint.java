package com.gienyne.industrialedgegateway.persistence.metric.models.api;


import java.time.Instant;


/**
 * One point of a metric history.
 * 
 * Numeric metrics: value = average of the time bucket, min / max = lowest and highest raw measurement inside that bucket.
 * Boolean / String metrics: value = the state, min and max are null.
 */
public record ApiHistoryPoint(Instant timestamp, Object value, Double min, Double max){
    
    public ApiHistoryPoint(Instant timestamp, Object value){
        this(timestamp, value, null, null);
    }
}