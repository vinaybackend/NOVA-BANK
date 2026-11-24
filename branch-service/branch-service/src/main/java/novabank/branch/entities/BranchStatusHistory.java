package novabank.branch.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "branch_status_history", indexes = {@Index(name = "idx_branch_history_branch", columnList = "branch_id"), @Index(name = "idx_branch_history_changed_at", columnList = "changed_at")})
public class BranchStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public BranchStatusHistory setId(Long id) {
        this.id = id;
        return this;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public BranchStatusHistory setBranchCode(String branchCode) {
        this.branchCode = branchCode;
        return this;
    }

    public BranchStatus getPreviousStatus() {
        return previousStatus;
    }

    public BranchStatusHistory setPreviousStatus(BranchStatus previousStatus) {
        this.previousStatus = previousStatus;
        return this;
    }

    public BranchStatus getNewStatus() {
        return newStatus;
    }

    public BranchStatusHistory setNewStatus(BranchStatus newStatus) {
        this.newStatus = newStatus;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public BranchStatusHistory setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public BranchStatusHistory setChangedBy(String changedBy) {
        this.changedBy = changedBy;
        return this;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public BranchStatusHistory setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
        return this;
    }

    @Column(name = "branch_id", nullable = false)
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 30)
    private BranchStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private BranchStatus newStatus;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "changed_by", nullable = false, length = 150)
    private String changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected BranchStatusHistory() {
    }

    public BranchStatusHistory(
            String branchCode,
            BranchStatus previousStatus,
            BranchStatus newStatus,
            String reason,
            String changedBy
    ) {
        this.branchCode = branchCode;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedBy = changedBy;
    }

    @PrePersist
    protected void onCreate() {
        changedAt = Instant.now();
    }
}