package com.nova.bank.auth.dto;

public record ErrorResponse(String message, int code, boolean success) {
}
