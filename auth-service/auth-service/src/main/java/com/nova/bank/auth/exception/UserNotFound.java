package com.nova.bank.auth.exception;

public class UserNotFound extends RuntimeException{
    public UserNotFound(String message){
        super(message);
    }
}
