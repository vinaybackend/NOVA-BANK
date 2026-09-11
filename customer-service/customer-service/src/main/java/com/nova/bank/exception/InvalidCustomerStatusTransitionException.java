package com.nova.bank.exception;

public class InvalidCustomerStatusTransitionException extends RuntimeException{
    public InvalidCustomerStatusTransitionException(String message){
        super(message);
    }
}
