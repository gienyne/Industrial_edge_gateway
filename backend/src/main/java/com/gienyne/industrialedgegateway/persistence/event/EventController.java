package com.gienyne.industrialedgegateway.persistence.event;


import com.gienyne.industrialedgegateway.persistence.domain.EventSeverity;
import com.gienyne.industrialedgegateway.persistence.domain.EventType;
import com.gienyne.industrialedgegateway.persistence.event.models.api.ApiEventListResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;


@RestController
@RequestMapping("/api/events")
public class EventController{

    private final EventQueryService eventQueryService;

    public EventController(EventQueryService eventQueryService){
        this.eventQueryService = eventQueryService;
    }


    @GetMapping
    public ApiEventListResponse listEvents(

        @RequestParam(name = "type", required = false)
        EventType type,

        @RequestParam(name = "severity", required = false)
        EventSeverity severity,

        @RequestParam(name = "deviceId", required = false)
        String deviceId,

        @RequestParam(name = "from", required = false)
        Instant from,

        @RequestParam(name = "to", required = false)
        Instant to,

        @RequestParam(name = "page", defaultValue = "0")
        int page,

        @RequestParam(name = "size", defaultValue = "100")
        int size
        
        ){

            return eventQueryService.search(type, severity, deviceId, from, to, page, size);

        }


}