package com.nova.bank.account.exceptions;

public class KycNotApprovedException extends RuntimeException{
    public KycNotApprovedException(String message){
        super(message);
    }
}
