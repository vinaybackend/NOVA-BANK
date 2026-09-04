package com.nova.bank.dto;


import com.nova.bank.entities.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateCustomerStatusRequest {

    @NotNull(message = "Customer status is required")
    private CustomerStatus status;

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }
}
