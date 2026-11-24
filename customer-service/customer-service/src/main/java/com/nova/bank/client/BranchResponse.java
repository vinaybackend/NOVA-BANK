package com.nova.bank.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchResponse {
    private Long id;
    private String branchCode;
    private String branchName;
    private String ifscCode;
    private String branchType;
    private String address;
    private String city;
    private String district;
    private String state;
    private String pinCode;
    private String country;
    private String phone;
    private String email;
    private String status;
    private String openingDate;
    private String openingTime;
    private String closingTime;
    private String timeZone;
}