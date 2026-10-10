package com.gienyne.industrialedgegateway.persistence.repository;


import com.gienyne.industrialedgegateway.persistence.domain.DeviceMetric;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface DeviceMetricRepository extends JpaRepository<DeviceMetric, Long>{
    List<DeviceMetric> findByDeviceId(String deviceId);
}