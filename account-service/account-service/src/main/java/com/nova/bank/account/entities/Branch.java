package com.nova.bank.account.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "branches", uniqueConstraints = {@UniqueConstraint(name = "uk_branch_id", columnNames = "branch_id"),
                @UniqueConstraint(name = "uk_branch_code", columnNames = "branch_code"),
                @UniqueConstraint(name = "uk_branch_ifsc", columnNames = "ifsc")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false, unique = true, length = 30)
    private String branchId;
    @Column(name = "branch_code", nullable = false, unique = true, length = 20)
    private String branchCode;

    @Column(name = "branch_name", nullable = false, length = 150)
    private String branchName;
    @Column(name = "ifsc", nullable = false, unique = true, length = 11)
    private String ifsc;

    @Column(name = "address", nullable = false, length = 250)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "country", nullable = false, length = 100)
    @Builder.Default
    private String country = "India";

    @Column(name = "pin_code", nullable = false, length = 10)
    private String pinCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BranchStatus status = BranchStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = BranchStatus.ACTIVE;
        }

        if (country == null) {
            country = "India";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}