package com.gienyne.industrialedgegateway.persistence.metric.models.api;


// Min, max and average are null for Boolean/String metrics.
public record ApiHistoryStatistics(Double min, Double max, Double average, Object latest){

}