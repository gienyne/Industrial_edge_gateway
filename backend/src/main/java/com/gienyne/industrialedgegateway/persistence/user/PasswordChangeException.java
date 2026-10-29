package com.gienyne.industrialedgegateway.persistence.user;


public class PasswordChangeException extends RuntimeException{

    // In case The password could not be changed;
    public PasswordChangeException(String message){
        super(message);
    }
}