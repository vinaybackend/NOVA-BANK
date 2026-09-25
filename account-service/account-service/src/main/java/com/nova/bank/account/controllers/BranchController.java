package com.nova.bank.account.controllers;

import com.nova.bank.account.dto.BranchResponse;
import com.nova.bank.account.dto.CreateBranchRequest;
import com.nova.bank.account.services.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<BranchResponse> createBranch(@Valid @RequestBody CreateBranchRequest request) {

        BranchResponse response = branchService.createBranch(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{branchId}")
    public ResponseEntity<BranchResponse> getBranch(@PathVariable String branchId) {

        BranchResponse response = branchService.getBranchById(branchId);

        return ResponseEntity.ok(response);
    }
}