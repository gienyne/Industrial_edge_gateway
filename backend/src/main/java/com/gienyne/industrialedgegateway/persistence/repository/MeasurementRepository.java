package com.gienyne.industrialedgegateway.persistence.repository;

import java.util.List;
import java.util.Optional;

import com.gienyne.industrialedgegateway.persistence.domain.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface MeasurementRepository extends JpaRepository<Measurement, Long>{

    boolean existsByDeviceIdAndMetricNameAndTime(String deviceId, String metricName, Instant time);

    // Finds the latest value for one metric.
    Optional<Measurement> findFirstByDeviceIdAndMetricNameOrderByTimeDesc(String deviceId, String metricName);

    // Finds the 20 newest measurements for one device.
    List<Measurement> findTop20ByDeviceIdOrderByTimeDesc(String deviceId);

}