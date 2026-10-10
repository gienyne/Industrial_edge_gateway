package com.gienyne.industrialedgegateway.persistence.repository;


import com.gienyne.industrialedgegateway.persistence.domain.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface MeasurementRepository extends JpaRepository<Measurement, Long>{

    boolean existsByDeviceIdAndMetricNameAndTime(String deviceId, String metricName, Instant time);

}