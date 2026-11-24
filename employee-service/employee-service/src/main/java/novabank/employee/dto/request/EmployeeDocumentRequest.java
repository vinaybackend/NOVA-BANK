package novabank.employee.dto.request;
import lombok.Data;
import novabank.employee.entities.enums.DocumentType;

@Data
public class EmployeeDocumentRequest {

    private DocumentType documentType;

    private String documentNumber;

}
