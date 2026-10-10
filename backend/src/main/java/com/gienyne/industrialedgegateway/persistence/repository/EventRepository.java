package com.gienyne.industrialedgegateway.persistence.repository;


import com.gienyne.industrialedgegateway.persistence.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EventRepository extends JpaRepository<Event, Long>{

}