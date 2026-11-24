package novabank.employee.dto.request;
import lombok.Data;

@Data
public class BankAccountRequest {


    private String accountNumber;

    private String ifscCode;


    private String bankName;

    private String accountHolderName;
}