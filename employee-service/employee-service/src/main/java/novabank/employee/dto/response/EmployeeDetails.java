package novabank.employee.dto.response;

import lombok.Builder;
import lombok.Data;
import novabank.employee.entities.enums.MaritalStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class EmployeeDetails {
    private Long id;
    private String employeeCode;
    private Long authUserId;

    private String name;
    private LocalDate dateOfBirth;
    private String gender;
    private String email;
    private String mobileNumber;

    private String jobTitle;
    private String department;
    private String grade;
    private String employeeType;
    private String employmentStatus;
    private MaritalStatus maritalStatus;

    // Branch Details
    private String branchCode;
    private String branchName; // 👈 Added

    private LocalDate dateOfJoining;
    private List<DocumentDto> documents;
    private BankAccountDto bankAccount;
    private Instant createdAt;
    private Instant updatedAt;

}
