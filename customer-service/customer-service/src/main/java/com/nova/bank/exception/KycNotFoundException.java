package com.nova.bank.exception;

public class KycNotFoundException extends RuntimeException {

    public KycNotFoundException(String message) {
        super(message);
    }
}
