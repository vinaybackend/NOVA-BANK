package com.nova.bank.account.exceptions;

public class AccountBlockedException
        extends RuntimeException {

    public AccountBlockedException(String message) {
        super(message);
    }
}