package com.nova.bank.auth.dto;

public record JwtResponse(
        String token ,
        String refreshToken,
        UserDto user
) {
}
