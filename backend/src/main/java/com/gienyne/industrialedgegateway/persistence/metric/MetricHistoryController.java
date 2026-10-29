package com.gienyne.industrialedgegateway.persistence.metric;


import java.time.Instant;

import com.gienyne.industrialedgegateway.persistence.metric.models.api.ApiMetricHistoryResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/metrics/history")
public class MetricHistoryController{

    private final MetricHistoryService metricHistoryService;

    public MetricHistoryController(MetricHistoryService metricHistoryService){
        this.metricHistoryService = metricHistoryService;
    }

    @GetMapping
    public ApiMetricHistoryResponse getHistory(
        @RequestParam String deviceId,
        @RequestParam String metricName,
        @RequestParam Instant from,
        @RequestParam Instant to
    ){
        return metricHistoryService.getHistory(
            deviceId,
            metricName,
            from,
            to
        );
    }
}