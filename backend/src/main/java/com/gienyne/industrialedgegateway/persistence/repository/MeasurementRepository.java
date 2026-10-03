package com.gienyne.industrialedgegateway.persistence.repository;


import com.gienyne.industrialedgegateway.persistence.domain.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;


public interface MeasurementRepository extends JpaRepository<Measurement, Long>{
    
}