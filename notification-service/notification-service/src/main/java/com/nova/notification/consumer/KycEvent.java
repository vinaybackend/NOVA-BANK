package com.nova.notification.consumer;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KycEvent {
    String name;
    String verificationStatus;
    String kycId;
    String branchName;
    LocalDateTime createdAt;
    String email;

}
