package nova.kyc.controller;

import nova.kyc.dto.KycResponse;
import nova.kyc.dto.KycDocumentFile;
import nova.kyc.dto.KycStatusResponse;
import nova.kyc.dto.KycStatusUpdateRequest;
import nova.kyc.dto.KycSubmitRequest;
import nova.kyc.entity.KycApplicationEntity;
import nova.kyc.service.KycService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/submit")
    public ResponseEntity<KycResponse> submitKyc(@RequestBody KycSubmitRequest request) {
        KycResponse response = kycService.submitKyc(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/submit/documents", consumes = "multipart/form-data")
    public ResponseEntity<KycResponse> submitKycDocuments(
            @RequestParam String customerId,
            @RequestParam String kycType,
            @RequestParam(required = false) String preferredBranchCode,
            @RequestParam String aadhaarNumber,
            @RequestParam String panNumber,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String mobileNumber,
            @RequestPart("aadhaarFile") MultipartFile aadhaarFile,
            @RequestPart("panFile") MultipartFile panFile) throws IOException {
        KycResponse response = kycService.submitKycDocuments(customerId, kycType, preferredBranchCode, aadhaarNumber, panNumber, aadhaarFile, panFile,email,name,mobileNumber);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','KYC_OFFICER','ADMIN','BRANCH_MANAGER')")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<KycResponse>> getKycByCustomerId(@PathVariable String customerId) {
        return ResponseEntity.ok(kycService.getKycByCustomerId(customerId));
    }

    @PreAuthorize("hasAnyRole('KYC_OFFICER','ADMIN','BRANCH_MANAGER')")
    @GetMapping("/all")
    public ResponseEntity<List<KycResponse>> getAllKyc() {
        return ResponseEntity.ok(kycService.getAllKyc());
    }

    @PreAuthorize("hasAnyRole('KYC_OFFICER','ADMIN','BRANCH_MANAGER')")
    @GetMapping("/{kycId}/documents/{documentId}")
    public ResponseEntity<Resource> getKycDocument(
            @PathVariable String kycId,
            @PathVariable String documentId) throws IOException {
        KycDocumentFile file = kycService.getDocument(kycId, documentId);
        MediaType contentType = file.fileName().toLowerCase().endsWith(".pdf")
                ? MediaType.APPLICATION_PDF
                : file.fileName().toLowerCase().endsWith(".png")
                        ? MediaType.IMAGE_PNG
                        : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(contentType)
                .header("Content-Disposition", "inline")
                .body(new ByteArrayResource(file.content()));
    }

    @PreAuthorize("hasAnyRole('KYC_OFFICER','ADMIN','BRANCH_MANAGER')")
    @PatchMapping("/{kycId}/status")
    public ResponseEntity<KycStatusResponse> updateKycStatus(@PathVariable String kycId, @RequestBody KycStatusUpdateRequest request) {
        return ResponseEntity.ok(kycService.updateKycStatus(kycId, request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @GetMapping("/all-kyc/{branchCode}")
    public ResponseEntity<?> branchKyc(@PathVariable("branchCode") String branchCode,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size){
        Page<KycApplicationEntity> kycApplicationEntities = kycService.branchKyc(branchCode, page, size);
        return new ResponseEntity<>(kycApplicationEntities,HttpStatus.OK);
    }

}
