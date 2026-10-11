package com.gienyne.industrialedgegateway.persistence.user.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;


@Entity
@Table(name = "users")
public class DashboardUser{

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DashboardUser(){

    }

    public String getUserId(){
        return userId;
    }

    public String getPasswordHash(){
        return passwordHash;
    }

    public boolean isActive(){
        return active;
    }

    public Instant getCreatedAt(){
        return createdAt;
    }
}