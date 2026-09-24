package com.nova.bank.dto;
import com.nova.bank.entities.KycType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KycRequest {

    @NotNull(message = "KYC type is required")
    private KycType kycType;

    @NotEmpty(message = "At least one KYC document is required")
    @Valid
    private List<KycDocumentRequest> documents;
}
