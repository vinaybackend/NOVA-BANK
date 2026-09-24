package com.nova.bank.entities;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String kycId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycAuditAction action;

    @Column(nullable = false)
    private String performedBy;

    private String performedByName;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    private String remarks;
}