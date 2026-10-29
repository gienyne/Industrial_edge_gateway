package com.gienyne.industrialedgegateway.persistence.repository;


import com.gienyne.industrialedgegateway.persistence.domain.DeviceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;


public interface DeviceMetricRepository extends JpaRepository<DeviceMetric, Long>{

    List<DeviceMetric> findByDeviceId(String deviceId);

    // Defines the device ID and metric count returned by the query
    interface MetricCount{
        String getDeviceId();
        long getTotal();
    }

    // Counts the metrics for each device in one query.
    @Query("select m.deviceId as deviceId, count(m) as total " + "from DeviceMetric m group by m.deviceId")
    List<MetricCount> metricCountPerDevice();
}