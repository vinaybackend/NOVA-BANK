package com.nova.bank.dto;
import com.nova.bank.entities.KycDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocumentRequest {

    @NotNull(message = "Document type is required")
    private KycDocumentType documentType;

    @NotBlank(message = "Document number is required")
    @Size(max = 100, message = "Document number must not exceed 100 characters")
    private String documentNumber;

    @NotBlank(message = "File name is required")
    private String fileName;
}