package com.nova.bank.kyc.exceptions;

public class KycAlreadyExistsException extends RuntimeException {

    public KycAlreadyExistsException(String message) {
        super(message);
    }
}