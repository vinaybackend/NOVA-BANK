package com.nova.bank.auth.dto;

public record LoginRequest(
        String username ,
        String password
) {
}
