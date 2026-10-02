package com.gienyne.industrialedgegateway.persistence.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;


/**
 * groupId and edgeNodeId are stored for information only, not as part of the
 * key. This is safe today because the gateway runs a single edge node in a single Sparkplug
 * group. If that ever changes, deviceId alone stops being a safe identity
 * and this class (and its uses) would need to move to the composite key.
 */
@Entity
@Table(name = "devices")
public class Device{

    @Id
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "edge_node_id", nullable = false)
    private String edgeNodeId;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;


    protected Device(){
        // required by JPA
    }


    public Device(String deviceId, String groupId, String edgeNodeId, Instant now){
        this.deviceId = deviceId;
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.firstSeenAt = now;
        this.lastSeenAt = now;
    }



    public void touch(Instant now){
        this.lastSeenAt = now;
    }

    public String getDeviceId(){
        return deviceId;
    }

    public String getGroupId(){
        return groupId;
    }

    public String getEdgeNodeId(){
        return edgeNodeId;
    }

    public Instant getFirstSeenAt(){
        return firstSeenAt;
    }

    public Instant getLastSeenAt(){
        return lastSeenAt;
    }
}
