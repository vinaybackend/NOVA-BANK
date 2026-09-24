package com.nova.bank.exception;

public class KycAlreadyExistsException extends RuntimeException {

    public KycAlreadyExistsException(String message) {
        super(message);
    }
}