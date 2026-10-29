package com.gienyne.industrialedgegateway.persistence.domain;


public enum EventType{

    EDGE_NODE_ONLINE(EventSeverity.INFO),
    EDGE_NODE_OFFLINE(EventSeverity.HIGH),
    DEVICE_ONLINE(EventSeverity.INFO),
    DEVICE_OFFLINE(EventSeverity.HIGH),
    REBIRTH_REQUESTED(EventSeverity.MEDIUM),

    SEQUENCE_GAP(EventSeverity.MEDIUM);

    private final EventSeverity severity;

    EventType(EventSeverity severity){
        this.severity = severity;
    }

    public EventSeverity severity(){
        return severity;
    }

}