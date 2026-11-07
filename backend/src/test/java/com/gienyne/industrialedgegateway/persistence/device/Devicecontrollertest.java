package com.gienyne.industrialedgegateway.persistence.device;


import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiDeviceDetailsResponse;
import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiDeviceSummaryResponse;
import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiMeasurementResponse;
import com.gienyne.industrialedgegateway.persistence.domain.EntityStatus;
import com.gienyne.industrialedgegateway.persistence.shared.error.GlobalExceptionHandler;
import com.gienyne.industrialedgegateway.persistence.shared.error.ResourceNotFoundException;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest{

    @Mock
    private DeviceService deviceService;

    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp(){

        mockMvc = MockMvcBuilders.standaloneSetup(new DeviceController(deviceService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }


    @Test
    void listDevices_returnsTheDevices() throws Exception{

        when(deviceService.listDevices()).thenReturn(List.of(
            new ApiDeviceSummaryResponse("pump", EntityStatus.ONLINE, null, 5),
            new ApiDeviceSummaryResponse("spritzgussmachine", EntityStatus.OFFLINE, null, 14)
        ));

        mockMvc.perform(get("/api/devices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].deviceId").value("pump"))
            .andExpect(jsonPath("$[0].status").value("ONLINE"))
            .andExpect(jsonPath("$[0].metricCount").value(5))
            .andExpect(jsonPath("$[1].status").value("OFFLINE"));

    }


    @Test
    void getDevice_returnsTheDetailsAndTheCurrentValues() throws Exception{

        when(deviceService.getDevice("pump")).thenReturn(new ApiDeviceDetailsResponse(
            "pump", EntityStatus.ONLINE, "SFM", "IndustrialEdgeGateway", null,
            List.of(new ApiMeasurementResponse(null, "tankLevel", "Double", 12.5, "L"))
        ));


        mockMvc.perform(get("/api/devices/{deviceId}", "pump"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deviceId").value("pump"))
            .andExpect(jsonPath("$.groupId").value("SFM"))
            .andExpect(jsonPath("$.edgeNodeId").value("IndustrialEdgeGateway"))
            .andExpect(jsonPath("$.currentValues[0].metricName").value("tankLevel"))
            .andExpect(jsonPath("$.currentValues[0].value").value(12.5))
            .andExpect(jsonPath("$.currentValues[0].unit").value("L"));
    }


    @Test
    void getDevice_unknown_returns404_withTheMessage() throws Exception{

        when(deviceService.getDevice("ghost")).thenThrow(new ResourceNotFoundException("Device was not found"));

        mockMvc.perform(get("/api/devices/{deviceId}", "ghost"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Device was not found"));

    }

    
    @Test
    void recentMeasurements_returnsTheMeasurements() throws Exception{

        when(deviceService.recentMeasurements("pump")).thenReturn(
            List.of(new ApiMeasurementResponse(null, "pumpActive", "Boolean", true, null))
        );

        mockMvc.perform(get("/api/devices/{deviceId}/measurements/recent", "pump"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].metricName").value("pumpActive"))
            .andExpect(jsonPath("$[0].value").value(true));

    }

}