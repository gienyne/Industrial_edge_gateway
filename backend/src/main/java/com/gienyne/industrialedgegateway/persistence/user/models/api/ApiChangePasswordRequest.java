package com.gienyne.industrialedgegateway.persistence.user.models.api;


public record ApiChangePasswordRequest(String currentPassword, String newPassword){
    
}