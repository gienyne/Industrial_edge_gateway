package com.gienyne.industrialedgegateway.persistence.metric;


import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gienyne.industrialedgegateway.persistence.metric.models.api.ApiHistoryPoint;
import com.gienyne.industrialedgegateway.persistence.metric.models.api.ApiHistoryStatistics;
import com.gienyne.industrialedgegateway.persistence.metric.models.api.ApiMetricHistoryResponse;
import com.gienyne.industrialedgegateway.persistence.shared.error.GlobalExceptionHandler;
import com.gienyne.industrialedgegateway.persistence.shared.error.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;



@ExtendWith(MockitoExtension.class)
class MetricHistoryControllerTest{

    private static final Instant FROM = Instant.parse("2026-10-22T10:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-22T11:00:00Z");

    @Mock
    private MetricHistoryService metricHistoryService;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp(){

        mockMvc = MockMvcBuilders.standaloneSetup(new MetricHistoryController(metricHistoryService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    }


    @Test
    void getHistory_returnsPointsWithMinMax_andStatistics() throws Exception{

        ApiMetricHistoryResponse history = new ApiMetricHistoryResponse(
            "pump", "tankLevel", "Double", "L", FROM, TO, 8, "time_bucket_average", 10L,
            List.of(new ApiHistoryPoint(FROM, 10.0, 8.0, 12.0)),
            new ApiHistoryStatistics(8.0, 22.0, 17.5, 21.5)
        );

        when(metricHistoryService.getHistory("pump", "tankLevel", FROM, TO)).thenReturn(history);

        mockMvc.perform(get("/api/metrics/history")
                .param("deviceId", "pump")
                .param("metricName", "tankLevel")
                .param("from", FROM.toString())
                .param("to", TO.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deviceId").value("pump"))
            .andExpect(jsonPath("$.unit").value("L"))
            .andExpect(jsonPath("$.aggregation").value("time_bucket_average"))
            .andExpect(jsonPath("$.bucketSeconds").value(10))
            .andExpect(jsonPath("$.points[0].value").value(10.0))
            .andExpect(jsonPath("$.points[0].min").value(8.0))
            .andExpect(jsonPath("$.points[0].max").value(12.0))
            .andExpect(jsonPath("$.statistics.min").value(8.0))
            .andExpect(jsonPath("$.statistics.max").value(22.0))
            .andExpect(jsonPath("$.statistics.average").value(17.5))
            .andExpect(jsonPath("$.statistics.latest").value(21.5));

    }


    @Test
    void getHistory_withAMissingParameter_returns400() throws Exception{

        mockMvc.perform(get("/api/metrics/history")
                .param("deviceId", "pump")
                .param("from", FROM.toString())
                .param("to", TO.toString()))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(metricHistoryService);

    }


    @Test
    void getHistory_withAnInvalidDate_returns400() throws Exception{

        mockMvc.perform(get("/api/metrics/history")
                .param("deviceId", "pump")
                .param("metricName", "tankLevel")
                .param("from", "yesterday")
                .param("to", TO.toString()))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(metricHistoryService);

    }


    @Test
    void getHistory_forAnUnknownMetric_returns404() throws Exception{

        when(metricHistoryService.getHistory("pump", "ghost", FROM, TO))
            .thenThrow(new ResourceNotFoundException("Metric was not found for this device"));

        
        mockMvc.perform(get("/api/metrics/history")
                .param("deviceId", "pump")
                .param("metricName", "ghost")
                .param("from", FROM.toString())
                .param("to", TO.toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Metric was not found for this device"));

    }


    @Test
    void getHistory_rejectedByTheService_returns400_withTheMessage() throws Exception{

        when(metricHistoryService.getHistory("pump", "tankLevel", TO, FROM))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "The start date must not be after the end date"));

        
        mockMvc.perform(get("/api/metrics/history")
                .param("deviceId", "pump")
                .param("metricName", "tankLevel")
                .param("from", TO.toString())
                .param("to", FROM.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("The start date must not be after the end date"));

    }
    
}