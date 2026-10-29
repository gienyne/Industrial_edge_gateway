package com.gienyne.industrialedgegateway.persistence.device.models.api;


import com.gienyne.industrialedgegateway.persistence.domain.EntityStatus;

import java.time.Instant;
import java.util.List;


// Returns device information and its latest known values.
public record ApiDeviceDetailsResponse(String deviceId, EntityStatus status, String groupId, String edgeNodeId, Instant lastSeen, List<ApiMeasurementResponse> currentValues){

}