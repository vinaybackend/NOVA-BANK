package com.nova.bank.controllers;
import com.nova.bank.dto.*;
import com.nova.bank.services.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kycs")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

//    @PostMapping("/create")
//    public ResponseEntity<KycResponse> createKyc(@Valid @RequestBody CreateKycRequest request) {
//
//        KycResponse response = kycService.createKyc(request);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    @GetMapping("/search/{kycId}")
//    public ResponseEntity<KycResponse> getKycByKycId(@PathVariable String kycId) {
//
//        KycResponse response = kycService.getKycById(kycId);
//
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<KycResponse> getKycByCustomerId(@PathVariable String customerId) {

        KycResponse response = kycService.getKycByCustomerId(customerId);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @PatchMapping("/review/{kycId}")
    public ResponseEntity<KycResponse> reviewKyc(@PathVariable String kycId, org.springframework.security.core.Authentication authentication) {

        KycResponse response = kycService.reviewKyc(kycId,authentication);

        return ResponseEntity.ok(response);
    }

    // reject
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @PatchMapping("/reject/{kycId}")
    public ResponseEntity<KycResponse> rejectKyc(@PathVariable String kycId, @Valid @RequestBody RejectKycRequest request, Authentication authentication) {

        KycResponse response = kycService.rejectKyc(kycId, request,authentication);

        return ResponseEntity.ok(response);
    }

    //approve
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @PatchMapping("/approve/{kycId}")
    public ResponseEntity<CustomerResponse> approveKyc(@PathVariable String kycId, org.springframework.security.core.Authentication authentication) {

        CustomerResponse customerResponse = kycService.approveKyc(kycId, authentication);

        return new ResponseEntity<>(customerResponse,HttpStatus.OK);
    }
    @GetMapping("/status/{customerId}")
    public KycStatusResponse getKycStatus(@PathVariable String customerId){
        KycStatusResponse kycStatusByCustomerId = (KycStatusResponse) kycService.getKycStatusByCustomerId(customerId);
        return kycStatusByCustomerId;
    }
//
//    @PatchMapping("/submit/{kycId}")
//    public ResponseEntity<KycResponse> submitKyc(@PathVariable String kycId) {
//
//        KycResponse response = kycService.submitKyc(kycId);
//
//        return ResponseEntity.ok(response);
//    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','KYC_OFFICER','ADMIN')")
    @PatchMapping("/resubmit/{kycId}")
    public KycResponse resubmitKyc(@PathVariable String kycId) {

        return kycService.resubmitKyc(kycId);
    }

    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','ADMIN','KYC_OFFICER')")
    @GetMapping("/pending")
    public PageResponse<KycResponse> pendingKyc(@RequestParam(name = "page",defaultValue = "0") int page,
                                                  @RequestParam(name = "size",defaultValue = "10") int size,
                                                  @RequestParam(name = "sortBy",defaultValue = "id") String sortBy,
                                                  @RequestParam(name = "sortDir",defaultValue = "asc") String sortDir){
        PageResponse<KycResponse> kycResponsePageResponse = kycService.allPendingKyc(page, size, sortBy, sortDir);
        return kycResponsePageResponse;
    }
}