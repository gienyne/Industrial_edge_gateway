package com.gienyne.industrialedgegateway.persistence.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


import java.time.Instant;


@Entity
@Table(name = "events")
public class Event{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time", nullable = false)
    private Instant time;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EventType type;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "edge_node_id", nullable = false)
    private String edgeNodeId;

    // null for Edge Node level events (EDGE_NODE_ONLINE / EDGE_NODE_OFFLINE / REBIRTH_REQUESTED).
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "message", nullable = false)
    private String message;


    protected Event(){
        // required by JPA
    }


    public Event(Instant time, EventType type, String groupId, String edgeNodeId, String deviceId, String message){

        this.time = time;
        this.type = type;
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.deviceId = deviceId;
        this.message = message;

    }

    public Long getId() {
         return id;
    }

    public Instant getTime() {
        return time;
    }

    public EventType getType() {
        return type;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getEdgeNodeId() {
        return edgeNodeId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getMessage() {
        return message;
    }
}