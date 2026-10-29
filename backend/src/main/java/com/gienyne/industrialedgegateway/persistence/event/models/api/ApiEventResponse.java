package com.gienyne.industrialedgegateway.persistence.event.models.api;


import com.gienyne.industrialedgegateway.persistence.domain.Event;
import com.gienyne.industrialedgegateway.persistence.domain.EventSeverity;
import com.gienyne.industrialedgegateway.persistence.domain.EventType;

import java.time.Instant;


public record ApiEventResponse(Long id, Instant timestamp, EventType type, EventSeverity severity, String groupId, String edgeNodeId, String deviceId, String message){

    public static ApiEventResponse from(Event event){

        return new ApiEventResponse(
            event.getId(),
            event.getTime(),
            event.getType(),
            event.getType().severity(),
            event.getGroupId(),
            event.getEdgeNodeId(),
            event.getDeviceId(),
            event.getMessage()
        );
    }
}
