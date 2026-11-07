package com.gienyne.industrialedgegateway.persistence.user.models.api;


import java.time.Instant;

public record ApiErrorResponse(int status, String error, String message, Instant timestamp){

}