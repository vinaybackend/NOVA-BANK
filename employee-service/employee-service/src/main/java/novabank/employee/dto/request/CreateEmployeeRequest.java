package novabank.employee.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import novabank.employee.entities.enums.EmployeeType;
import novabank.employee.entities.enums.MaritalStatus;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateEmployeeRequest {

    @NotBlank
    private String name;
    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotBlank
    private String gender;

    private MaritalStatus maritalStatus;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String mobileNumber;

    @NotBlank
    private String jobTitle;

    @NotBlank
    private String department;

    private String grade;

    @NotNull(message = "Employee type is required")
    private EmployeeType employeeType;

    private String branchCode;

    private String branchName;

    private String ifscCode;
    @NotBlank
    private String roleName;

    @Valid
    private BankAccountRequest bankAccount;

    @Valid
    private List<EmployeeDocumentRequest> documents;
}