package com.gienyne.industrialedgegateway.persistence.overview.models.api;


import java.util.List;

import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiDeviceSummaryResponse;
import com.gienyne.industrialedgegateway.persistence.event.models.api.ApiEventResponse;


public record ApiOverviewResponse(
    SystemHealth systemHealth,
    ConnectionStatus mqttConnection,
    ConnectionStatus databaseConnection,
    boolean synchronizing,
    int totalDevices,
    int onlineDevices,
    int offlineDevices,
    int unknownDevices,
    List<ApiDeviceSummaryResponse> devices,
    List<ApiEventResponse> recentActivities
){ 
}