package com.gienyne.industrialedgegateway.persistence.user.repository;


import com.gienyne.industrialedgegateway.persistence.user.domain.DashboardUser;
import org.springframework.data.jpa.repository.JpaRepository;


public interface DashboardUserRepository extends JpaRepository<DashboardUser, String>{

}