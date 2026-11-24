package com.nova.bank.account.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = {
                @Index(name = "idx_account_id", columnList = "account_id", unique = true),
                @Index(name = "idx_account_number", columnList = "account_number", unique = true),
                @Index(name = "idx_account_customer_id", columnList = "customer_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    private String email;

    private String name;

    @Column(name = "account_id", nullable = false, unique = true, length = 30)
    private String accountId;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    private String customerName;

    @Column(name = "customer_id", nullable = false, length = 30)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "branch_id", nullable = false, length = 30)
    private String branchCode;

    @Column(name = "branch_name", nullable = false, length = 150)
    private String branchName;

    @Column(name = "ifsc", nullable = false, length = 11)
    private String ifsc;

    @Column(name = "debit_card_status", length = 20)
    private String debitCardStatus;

    @Column(name = "debit_card_reference", unique = true, length = 40)
    private String debitCardNumber;

    @Column(name = "debit_card_last_four_digits", length = 4)
    private String debitCardLastFourDigits;

    @Column(name = "debit_card_type", length = 20)
    private String debitCardType;

    @Column(name = "debit_card_issued_at")
    private LocalDateTime debitCardIssuedAt;

    @Column(name = "debit_card_expiry_date")
    private LocalDate debitCardExpiryDate;

    @PrePersist
    private void prepareDebitCardPreview() {
        generateTestCardNumber();
        issueDebitCardPreviewIfMissing();

    }
    public void generateTestCardNumber() {
        SecureRandom random = new SecureRandom();
        StringBuilder cardNumber = new StringBuilder();

        for (int i = 0; i < 16; i++) {
            cardNumber.append(random.nextInt(10));
        }

        String debit= cardNumber.toString().replaceAll(
                "(\\d{4})(\\d{4})(\\d{4})(\\d{4})",
                "$1 $2 $3 $4"
        );
        debitCardNumber=debit;
    }

    public void issueDebitCardPreviewIfMissing() {
        if (debitCardStatus != null) {
            return;
        }
        debitCardStatus = "PREVIEW";
        debitCardType = "VIRTUAL";
        debitCardIssuedAt = LocalDateTime.now();
        debitCardExpiryDate = YearMonth.now().plusYears(5).atEndOfMonth();
        debitCardLastFourDigits = debitCardNumber.substring(debitCardNumber.length() - 4);
    }
}