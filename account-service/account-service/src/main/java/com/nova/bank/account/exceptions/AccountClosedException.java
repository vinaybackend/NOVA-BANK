package com.nova.bank.account.exceptions;

public class AccountClosedException extends RuntimeException {

    public AccountClosedException(String message) {
        super(message);
    }
}