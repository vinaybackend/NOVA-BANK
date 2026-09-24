package com.nova.bank.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class UserDto {
    @NotBlank
    private String name;
    @Email
    private String email;
    @NotBlank(message = "password required min 5 (character and digit.")
    private String password;
    @NotBlank(message = "mobile number required ")
    @Size(min = 10,max = 10,message = "10 digit required")
    private String phone;
    private LocalDateTime createAt;
}
