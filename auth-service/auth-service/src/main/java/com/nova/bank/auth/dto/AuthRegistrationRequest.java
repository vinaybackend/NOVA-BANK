package com.nova.bank.auth.dto;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRegistrationRequest {
    private String name;
    private String email;
    @Size(min = 10,max = 10,message = "10 digit required")
    private String phone;
    private String password;
    private String branchCode;
    private String branchName;
    private String ifsc;
    private String roleName;
}