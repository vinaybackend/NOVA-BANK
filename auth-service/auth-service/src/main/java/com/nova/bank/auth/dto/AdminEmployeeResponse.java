package com.nova.bank.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminEmployeeResponse {
    private Long id;
    private String name;
    private String email;
    @Size(min = 10,max = 10,message = "10 digit required")
    private String phone;
    private LocalDateTime createAt;
    private List<String> roles;
}
