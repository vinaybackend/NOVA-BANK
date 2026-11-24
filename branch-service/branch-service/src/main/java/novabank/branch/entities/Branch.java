package novabank.branch.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "branches", uniqueConstraints = {@UniqueConstraint(name = "uk_branch_code", columnNames = "branch_code"), @UniqueConstraint(name = "uk_branch_ifsc", columnNames = "ifsc_code")},
        indexes = {
                @Index(name = "idx_branch_city", columnList = "city"),
                @Index(name = "idx_branch_status", columnList = "status"),
                @Index(name = "idx_branch_name", columnList = "branch_name")
        }
)
@AllArgsConstructor
@Data
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Column(name = "branch_name", nullable = false, length = 120)
    private String branchName;

    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "branch_type", nullable = false, length = 30)
    private BranchType branchType;

    @Column(name = "address", nullable = false, length = 150)
    private String address;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String district;

    @Column(nullable = false, length = 80)
    private String state;

    @Column(name = "pin_code", nullable = false, length = 10)
    private String pinCode;

    @Column(nullable = false, length = 60)
    private String country = "India";

    @Column(length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BranchStatus status = BranchStatus.ACTIVE;

    @Column(name = "opening_date", nullable = false)
    private LocalDate openingDate;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone = "Asia/Kolkata";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(nullable = false)
    private Long version;

    public Branch() {
        // Required by JPA.
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}