package com.gienyne.industrialedgegateway.persistence.device.models.api;


import com.gienyne.industrialedgegateway.persistence.domain.EntityStatus;
import java.time.Instant;


// Data returned for one device in the device list.
public record ApiDeviceSummaryResponse(String deviceId, EntityStatus status, Instant lastSeen, long metricCount){

}