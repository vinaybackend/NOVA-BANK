package com.nova.bank.exception;

public class InvalidKycStatusException extends RuntimeException {

    public InvalidKycStatusException(String message) {
        super(message);
    }
}