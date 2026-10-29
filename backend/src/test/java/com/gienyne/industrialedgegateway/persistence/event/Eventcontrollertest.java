package com.gienyne.industrialedgegateway.persistence.event;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gienyne.industrialedgegateway.persistence.domain.EventSeverity;
import com.gienyne.industrialedgegateway.persistence.domain.EventType;
import com.gienyne.industrialedgegateway.persistence.event.models.api.ApiEventListResponse;
import com.gienyne.industrialedgegateway.persistence.event.models.api.ApiEventResponse;
import com.gienyne.industrialedgegateway.persistence.shared.error.GlobalExceptionHandler;

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
class EventControllerTest{

    @Mock
    private EventQueryService eventQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(){

        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventQueryService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    }


    private static ApiEventListResponse emptyPage(){
        return new ApiEventListResponse(List.of(), 0, 100, 0, 0, false);
    }


    @Test
    void listEvents_withoutParameters_usesTheDefaults() throws Exception{

        when(eventQueryService.search(any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(emptyPage());

        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events.length()").value(0))
            .andExpect(jsonPath("$.hasNext").value(false));

        verify(eventQueryService).search(null, null, null, null, null, 0, 100);

    }


    @Test
    void listEvents_passesEveryFilterToTheService() throws Exception{

        when(eventQueryService.search(any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(emptyPage());

        mockMvc.perform(get("/api/events")
                .param("type", "DEVICE_OFFLINE")
                .param("severity", "HIGH")
                .param("deviceId", "pump")
                .param("from", "2026-10-22T10:00:00Z")
                .param("to", "2026-10-22T12:00:00Z")
                .param("page", "2")
                .param("size", "25"))
            .andExpect(status().isOk());

        
        verify(eventQueryService).search(
            EventType.DEVICE_OFFLINE,
            EventSeverity.HIGH,
            "pump",
            Instant.parse("2026-10-22T10:00:00Z"),
            Instant.parse("2026-10-22T12:00:00Z"),
            2,
            25
        );

    }


    @Test
    void listEvents_returnsTheEventsWithTheirSeverity() throws Exception{

        ApiEventResponse event = new ApiEventResponse(7L, Instant.parse("2026-10-22T10:00:00Z"), EventType.DEVICE_OFFLINE, EventSeverity.HIGH,
            "SFM", "IndustrialEdgeGateway", "pump", "Device Offline");

        when(eventQueryService.search(any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new ApiEventListResponse(List.of(event), 0, 100, 1, 1, false));

        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.events[0].id").value(7))
            .andExpect(jsonPath("$.events[0].type").value("DEVICE_OFFLINE"))
            .andExpect(jsonPath("$.events[0].severity").value("HIGH"))
            .andExpect(jsonPath("$.events[0].deviceId").value("pump"))
            .andExpect(jsonPath("$.events[0].message").value("Device Offline"));

    }


    @Test
    void listEvents_withAnUnknownType_returns400() throws Exception{

        mockMvc.perform(get("/api/events").param("type", "NOT_A_TYPE"))
            .andExpect(status().isBadRequest());

    }


    @Test
    void listEvents_withAnUnknownSeverity_returns400() throws Exception{

        mockMvc.perform(get("/api/events").param("severity", "CRITICAL"))
            .andExpect(status().isBadRequest());
    }


    @Test
    void listEvents_rejectedByTheService_returns400_withTheMessage() throws Exception{

        when(eventQueryService.search(any(), any(), any(), any(), any(), anyInt(), anyInt()))
           .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater"));

        
        mockMvc.perform(get("/api/events").param("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Page must be zero or greater"));

    }
    
}