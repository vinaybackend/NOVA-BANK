package novabank.employee.dto.response;

import lombok.Data;
import novabank.employee.entities.enums.DocumentType;

@Data
public class DocumentDto {
    private DocumentType documentType;
    private String documentNumber;
    private String verificationStatus;
}