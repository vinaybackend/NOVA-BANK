package com.nova.bank.entities;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(name = "uk_customer_customer_id", columnNames = "customer_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true, length = 30, updatable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "title", length = 10)
    private Title title;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "nationality", nullable = false, length = 50)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 20)
    private MaritalStatus maritalStatus;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "mobile_number", length = 15)
    private String mobileNumber;

    @Column(name = "alternate_mobile_number", length = 15)
    private String alternateMobileNumber;

    @Column(name = "permanent_address", nullable = false, length = 150)
    private String permanentAddress;

    @Column(name = "permanent_city", nullable = false, length = 100)
    private String permanentCity;

    @Column(name = "permanent_state", nullable = false, length = 100)
    private String permanentState;

    @Column(name = "permanent_country", nullable = false, length = 100)
    private String permanentCountry;

    @Column(name = "permanent_pin_code", nullable = false, length = 10)
    private String permanentPinCode;


    @Column(name = "communication_address", length = 150)
    private String communicationAddress;

    @Column(name = "communication_city", length = 100)
    private String communicationCity;

    @Column(name = "communication_state", length = 100)
    private String communicationState;

    @Column(name = "communication_country", length = 100)
    private String communicationCountry;

    @Column(name = "communication_pin_code", length = 10)
    private String communicationPinCode;


    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 30)
    private EmploymentType employmentType;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "employer_name", length = 150)
    private String employerName;

    @Column(name = "annual_income", precision = 19, scale = 2)
    private BigDecimal annualIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_of_income", length = 30)
    private SourceOfIncome sourceOfIncome;


    @Column(name = "language", length = 30)
    private String language;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = CustomerStatus.PENDING;
        }

        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}