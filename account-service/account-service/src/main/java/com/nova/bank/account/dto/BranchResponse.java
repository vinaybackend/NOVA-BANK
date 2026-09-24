package com.nova.bank.account.dto;

import com.nova.bank.account.entities.BranchStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BranchResponse {

    private String branchId;
    private String branchCode;
    private String branchName;
    private String ifsc;

    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;

    private BranchStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}