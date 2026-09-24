package com.nova.bank.auth.dto;

import com.nova.bank.auth.entites.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminRegisterUser {
    @NotBlank
    private String name;
    @Email
    private String email;
    @NotBlank(message = "password required min 5 (character and digit.")
    private String password;
    @NotBlank(message = "mobile number required ")
    @Size(min = 10, max = 10, message = "10 digit required")
    private String phone;
    private LocalDateTime createAt;
    private Role role;
}
