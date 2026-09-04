package com.nova.bank.kyc.controllers;

import com.nova.bank.kyc.dto.CreateKycRequest;
import com.nova.bank.kyc.dto.KycResponse;
import com.nova.bank.kyc.dto.RejectKycRequest;
import com.nova.bank.kyc.dto.ReviewKycRequest;
import com.nova.bank.kyc.services.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kycs")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping("/create")
    public ResponseEntity<KycResponse> createKyc(@Valid @RequestBody CreateKycRequest request) {

        KycResponse response = kycService.createKyc(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/searchByCustomerId/{kycId}")
    public ResponseEntity<KycResponse> getKycById(@PathVariable String kycId) {

        KycResponse response = kycService.getKycById(kycId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<KycResponse> getKycByCustomerId(@PathVariable String customerId) {

        KycResponse response = kycService.getKycByCustomerId(customerId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/review/{kycId}")
    public ResponseEntity<KycResponse> reviewKyc(@PathVariable String kycId, @Valid @RequestBody ReviewKycRequest request) {

        KycResponse response = kycService.reviewKyc(kycId, request);

        return ResponseEntity.ok(response);
    }

    // reject
    @PatchMapping("/reject/{kycId}")
    public ResponseEntity<KycResponse> rejectKyc(@PathVariable String kycId, @Valid @RequestBody RejectKycRequest request) {

        KycResponse response = kycService.rejectKyc(kycId, request);

        return ResponseEntity.ok(response);
    }

    //approve

    @PatchMapping("/approve/{kycId}")
    public ResponseEntity<KycResponse> approveKyc(@PathVariable String kycId) {

        KycResponse response = kycService.approveKyc(kycId);

        return ResponseEntity.ok(response);
    }
}