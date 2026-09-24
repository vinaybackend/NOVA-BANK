package com.nova.bank.account.services;

import com.nova.bank.account.dto.BranchResponse;
import com.nova.bank.account.dto.CreateBranchRequest;
import com.nova.bank.account.entities.Branch;
import com.nova.bank.account.entities.BranchStatus;
import com.nova.bank.account.repositories.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional
    public BranchResponse createBranch(CreateBranchRequest request) {

        // Check duplicate branch code
        if (branchRepository.existsByBranchCode(request.getBranchCode())) {
            throw new IllegalArgumentException("Branch code already exists: " + request.getBranchCode());
        }
        // Check duplicate IFSC
        if (branchRepository.existsByIfsc(request.getIfsc())) {
            throw new IllegalArgumentException("IFSC already exists: " + request.getIfsc());
        }

        //Generate internal branch ID
        String branchId = generateBranchId();

        // 4. Create entity
        Branch branch = Branch.builder()
                .branchId(branchId)
                .branchCode(request.getBranchCode().toUpperCase())
                .branchName(request.getBranchName())
                .ifsc(request.getIfsc().toUpperCase())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pinCode(request.getPinCode())
                .status(BranchStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 5. Save
        Branch savedBranch = branchRepository.save(branch);

        // 6. Convert entity → response
        return mapToResponse(savedBranch);
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranchById(String branchId) {

        Branch branch = branchRepository.findByBranchId(branchId).orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));

        return mapToResponse(branch);
    }

    private String generateBranchId() {

        return "BR-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 10)
                        .toUpperCase();
    }

    private BranchResponse mapToResponse(Branch branch) {

        return BranchResponse.builder()
                .branchId(branch.getBranchId())
                .branchCode(branch.getBranchCode())
                .branchName(branch.getBranchName())
                .ifsc(branch.getIfsc())
                .address(branch.getAddress())
                .city(branch.getCity())
                .state(branch.getState())
                .country(branch.getCountry())
                .postalCode(branch.getPinCode())
                .status(branch.getStatus())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }
}