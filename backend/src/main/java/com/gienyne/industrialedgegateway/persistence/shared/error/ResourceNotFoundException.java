package com.gienyne.industrialedgegateway.persistence.shared.error;


public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(String message){
        super(message);
    }
}