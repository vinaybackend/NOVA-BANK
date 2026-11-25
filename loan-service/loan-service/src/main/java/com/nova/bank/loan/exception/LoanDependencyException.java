package com.nova.bank.loan.exception;

public class LoanDependencyException extends RuntimeException {

    public LoanDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
