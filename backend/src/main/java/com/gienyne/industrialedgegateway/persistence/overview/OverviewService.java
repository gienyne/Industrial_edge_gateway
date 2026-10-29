package com.gienyne.industrialedgegateway.persistence.overview;


import java.util.List;

import com.gienyne.industrialedgegateway.persistence.device.DeviceService;
import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiDeviceSummaryResponse;
import com.gienyne.industrialedgegateway.persistence.domain.EntityStatus;
import com.gienyne.industrialedgegateway.persistence.event.EventQueryService;
import com.gienyne.industrialedgegateway.persistence.event.models.api.ApiEventResponse;
import com.gienyne.industrialedgegateway.persistence.ingestion.SparkplugMessageProcessor;
import com.gienyne.industrialedgegateway.persistence.ingestion.SparkplugMqttListener;
import com.gienyne.industrialedgegateway.persistence.overview.models.api.ApiOverviewResponse;
import com.gienyne.industrialedgegateway.persistence.overview.models.api.ConnectionStatus;
import com.gienyne.industrialedgegateway.persistence.overview.models.api.SystemHealth;


import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

@Service
public class OverviewService{

    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final DeviceService deviceService;
    private final EventQueryService eventQueryService;
    private final SparkplugMqttListener mqttListener;
    private final SparkplugMessageProcessor messageProcessor;

    public OverviewService(
        DeviceService deviceService,
        EventQueryService eventQueryService,
        SparkplugMqttListener mqttListener,
        SparkplugMessageProcessor messageProcessor
    ){
        this.deviceService = deviceService;
        this.eventQueryService = eventQueryService;
        this.mqttListener = mqttListener;
        this.messageProcessor = messageProcessor;
    }


    public ApiOverviewResponse getOverview(){

        ConnectionStatus mqtt = mqttListener.isConnected() ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED;

        ConnectionStatus database = ConnectionStatus.CONNECTED;

        List<ApiDeviceSummaryResponse> devices;
        List<ApiEventResponse> recentActivities;

        try{
            devices = deviceService.listDevices();
            recentActivities = eventQueryService.find(null, null, null, null, RECENT_ACTIVITY_LIMIT);
        }
        catch(DataAccessException | TransactionException exception){
            database = ConnectionStatus.DISCONNECTED;
            devices = List.of();
            recentActivities = List.of();
        }

        SystemHealth health = mqtt == ConnectionStatus.CONNECTED && database == ConnectionStatus.CONNECTED ? SystemHealth.HEALTHY : SystemHealth.DEGRADED;

        return new ApiOverviewResponse(
            health,
            mqtt,
            database,
            messageProcessor.isSynchronizing(),
            devices.size(),
            count(devices, EntityStatus.ONLINE),
            count(devices, EntityStatus.OFFLINE),
            count(devices, EntityStatus.UNKNOWN),
            devices,
            recentActivities
        );
    }


    private static int count(List<ApiDeviceSummaryResponse> devices, EntityStatus status){

        return (int) devices.stream().filter(device -> device.status() == status).count();
    }

}