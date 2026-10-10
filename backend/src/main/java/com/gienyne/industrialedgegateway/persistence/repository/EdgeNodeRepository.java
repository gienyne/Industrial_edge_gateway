package com.gienyne.industrialedgegateway.persistence.repository;


import com.gienyne.industrialedgegateway.persistence.domain.EntityStatus;
import com.gienyne.industrialedgegateway.persistence.domain.EdgeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;


public interface EdgeNodeRepository extends JpaRepository<EdgeNode, Long>{

    Optional<EdgeNode> findByGroupIdAndEdgeNodeId(String groupId, String edgeNodeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EdgeNode n set n.status = :to where n.status = :from")
    int replaceStatus(@Param("from") EntityStatus from, @Param("to") EntityStatus to);

}