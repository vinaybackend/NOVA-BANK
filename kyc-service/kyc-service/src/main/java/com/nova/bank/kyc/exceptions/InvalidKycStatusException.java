package com.nova.bank.kyc.exceptions;

public class InvalidKycStatusException extends RuntimeException {

    public InvalidKycStatusException(String message) {
        super(message);
    }
}