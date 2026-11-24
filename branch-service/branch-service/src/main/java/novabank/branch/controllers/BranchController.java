package novabank.branch.controllers;

import jakarta.validation.Valid;
import novabank.branch.dto.request.BranchCreateRequest;
import novabank.branch.dto.request.BranchStatusUpdateRequest;
import novabank.branch.dto.request.BranchUpdateRequest;
import novabank.branch.dto.response.BranchResponse;
import novabank.branch.entities.BranchStatus;
import novabank.branch.entities.BranchStatusHistory;
import novabank.branch.entities.BranchType;
import novabank.branch.service.BranchService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BranchResponse> createBranch(@Valid @RequestBody BranchCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.createBranch(request));
    }

    @GetMapping("/{branchCode}")
    public ResponseEntity<BranchResponse> getBranchCode(@PathVariable String branchCode) {
        return ResponseEntity.ok(branchService.getBranchCode(branchCode));
    }

    @GetMapping
    public ResponseEntity<Page<BranchResponse>> getBranches(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BranchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(branchService.getBranches(city, status, page, size));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<BranchResponse> updateBranch(@PathVariable Long id, @Valid @RequestBody BranchUpdateRequest request) {
        return ResponseEntity.ok(branchService.updateBranch(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<BranchResponse> updateBranchStatus(@PathVariable Long id, @Valid @RequestBody BranchStatusUpdateRequest request) {
        return ResponseEntity.ok(branchService.updateBranchStatus(id, request));
    }

    @GetMapping("/{branchCode}/status-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<List<BranchStatusHistory>> getBranchStatusHistory(@PathVariable String branchCode) {
        return ResponseEntity.ok(branchService.getBranchStatusHistory(branchCode));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'KYC_OFFICER','CUSTOMER')")
    public ResponseEntity<Page<BranchResponse>> searchBranches(
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String ifscCode,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BranchStatus status,
            @RequestParam(required = false) BranchType branchType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(branchService.searchBranches(branchCode, ifscCode, city, status, branchType, page, size));
    }
}