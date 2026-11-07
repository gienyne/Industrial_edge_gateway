package com.gienyne.industrialedgegateway.persistence.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "edge_nodes")
public class EdgeNode{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "edge_node_id", nullable = false)
    private String edgeNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EntityStatus status = EntityStatus.UNKNOWN;

    /**
     * bdSeq of the last Edge Node session observed by the backend.
     * It is used to identify the session across backend restarts.
     */
    @Column(name = "bd_seq")
    private Long bdSeq;

    protected EdgeNode(){

    }

    public EdgeNode(String groupId, String edgeNodeId){

        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;

    }

    public Long getId(){
        return id;
    }

    public String getGroupId(){
        return groupId;
    }

    public String getEdgeNodeId(){
        return edgeNodeId;
    }

    public EntityStatus getStatus(){
        return status;
    }

    public void setStatus(EntityStatus status){
        this.status = status;
    }

    public Long getBdSeq(){
        return bdSeq;
    }

    public void setBdSeq(Long bdSeq){
        this.bdSeq = bdSeq;
    }
}
