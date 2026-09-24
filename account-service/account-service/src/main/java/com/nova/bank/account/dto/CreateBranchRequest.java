package com.nova.bank.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBranchRequest {

    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Branch code must not exceed 20 characters")
    private String branchCode;

    @NotBlank(message = "Branch name is required")
    @Size(max = 150, message = "Branch name must not exceed 150 characters")
    private String branchName;

    @NotBlank(message = "IFSC is required")
    @Size(min = 4, max = 6, message = "IFSC must be exactly 11 characters")
    private String ifsc;

    @NotBlank(message = "Address is required")
    @Size(max = 250, message = "Address must not exceed 250 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @NotBlank(message = "Postal code is required")
    @Size(max = 10, message = "Postal code must not exceed 10 characters")
    private String pinCode;
}