package com.nova.bank.account.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "account_holders",
        uniqueConstraints = {@UniqueConstraint(name = "uk_account_customer", columnNames = {"account_id", "customer_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 30)
    private String accountId;

    @Column(name = "customer_id", nullable = false, length = 30)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "holder_type", nullable = false, length = 20)
    private AccountHolderType holderType;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {

        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
    }
}