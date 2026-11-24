package novabank.employee.dto.response;
import lombok.Data;

@Data
public class BankAccountDto {
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String accountHolderName;
}