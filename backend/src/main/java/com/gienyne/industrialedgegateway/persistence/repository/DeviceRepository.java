package com.gienyne.industrialedgegateway.persistence.repository;

import java.util.List;

import com.gienyne.industrialedgegateway.persistence.domain.Device;
import com.gienyne.industrialedgegateway.persistence.domain.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface DeviceRepository extends JpaRepository<Device, String>{
    
    List<Device> findByGroupIdAndEdgeNodeId(String groupId, String edgeNodeId);

    long countByStatus(EntityStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Device d set d.status = :to where d.status = :from")
    int replaceStatus(@Param("from") EntityStatus from, @Param("to") EntityStatus to);

}