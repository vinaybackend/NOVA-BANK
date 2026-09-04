package com.nova.bank.kyc.exceptions;

public class KycNotFoundException extends RuntimeException {

    public KycNotFoundException(String message) {
        super(message);
    }
}
