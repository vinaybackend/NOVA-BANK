
package novabank.employee.services;
import lombok.RequiredArgsConstructor;
import novabank.employee.client.AuthClient;
import novabank.employee.client.AuthRegistrationRequest;
import novabank.employee.client.BranchClient;
import novabank.employee.client.BranchResponse;
import novabank.employee.dto.request.BankAccountRequest;
import novabank.employee.dto.request.CreateEmployeeRequest;
import novabank.employee.dto.response.BankAccountDto;
import novabank.employee.dto.response.DocumentDto;
import novabank.employee.dto.response.EmployeeDetails;
import novabank.employee.dto.response.EmployeeResponse;
import novabank.employee.entities.*;
import novabank.employee.entities.enums.*;
import novabank.employee.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BranchClient branchClient;
    private final AuthClient authClient;


    @Override
    @Transactional
    public EmployeeResponse registerEmployee(CreateEmployeeRequest request) {

        String email = request.getEmail().trim();

        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Employee with this email already exists.");
        }

        BranchResponse branch;
        try {
            branch = branchClient.getBranchById(request.getBranchCode().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to validate branch code: " + request.getBranchCode(), e);
        }

        if (branch == null || branch.branchCode() == null) {
            throw new IllegalArgumentException("Invalid branch code.");
        }

        // 👈 1. Generate a temporary password
        String generatedPassword = generateSecurePassword();

        // 👈 2. Create Auth Account
        AuthRegistrationRequest authReq = new AuthRegistrationRequest();
        authReq.setName(request.getName());
        authReq.setEmail(email);
        authReq.setPhone(request.getMobileNumber());
        authReq.setPassword(generatedPassword);
        authReq.setRoleName(request.getRoleName());
        authReq.setBranchName(request.getBranchName());
        authReq.setIfsc(request.getIfscCode());
        authReq.setBranchCode(request.getBranchCode());

        Long authUserId;
        try {
            // Auth Service returns the generated User ID as a String
            authUserId = authClient.registerEmployeeAuth(authReq);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create auth account: " + e.getMessage(), e);
        }

        Instant now = Instant.now();

        Employee employee = Employee.builder()
                .employeeCode(generateEmployeeCode())
                .authUserId(authUserId) // 👈 Use returned auth ID
                .name(request.getName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(Gender.valueOf(request.getGender().trim().toUpperCase(Locale.ROOT)))
                .maritalStatus(request.getMaritalStatus())
                .email(email)
                .mobileNumber(request.getMobileNumber().trim())
                .jobTitle(request.getJobTitle().trim())
                .department(request.getDepartment().trim())
                .grade(request.getGrade())
                .employmentStatus(EmploymentStatus.ONBOARDING)
                .branchCode(branch.branchCode())
                .branchName(branch.branchName())
                .ifsc(branch.ifscCode())
                .employeeType(request.getEmployeeType())
                .dateOfJoining(LocalDate.now())
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (request.getBankAccount() != null) {
            BankAccountRequest bankRequest = request.getBankAccount();
            EmployeeBankAccount bankAccount = EmployeeBankAccount.builder()
                    .employee(employee)
                    .accountNumber(bankRequest.getAccountNumber())
                    .ifscCode(bankRequest.getIfscCode())
                    .bankName(bankRequest.getBankName())
                    .accountHolderName(bankRequest.getAccountHolderName())
                    .build();
            employee.setBankAccount(bankAccount);
        }

        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            List<EmployeeDocument> documents = request.getDocuments().stream().map(d ->
                            EmployeeDocument.builder()
                                    .employee(employee)
                                    .documentType(d.getDocumentType())
                                    .documentNumber(d.getDocumentNumber())
                                    .verificationStatus(VerificationStatus.PENDING)
                                    .uploadedAt(now)
                                    .build())
                    .toList();
            employee.setDocuments(documents);
        }

        Employee savedEmployee = employeeRepository.save(employee);

        // 👈 3. Return the response with the generated password
        EmployeeResponse response = mapToResponse(savedEmployee);
        response.setGeneratedPassword(generatedPassword);

        return response;
    }

    // ... (Keep your other methods: getEmployeeById, getAllEmployees, updateEmployeeStatus, mapToResponse, generateEmployeeCode) ...

    // 👈 Helper to generate a secure random password
    private String generateSecurePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return password.toString();
    }


    // GET EMPLOYEE BY DATABASE ID
    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {

        Employee employee = employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

        return mapToResponse(employee);
    }

    // GET EMPLOYEE BY EMPLOYEE CODE
    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByCode(String employeeCode) {

        Employee employee = employeeRepository.findByEmployeeCodeIgnoreCase(employeeCode).orElseThrow(() -> new RuntimeException("Employee not found with code: " + employeeCode));

        return mapToResponse(employee);
    }

    // GET ALL EMPLOYEES
    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(String search, String branchCode, String status, Pageable pageable) {

        // This preserves your existing repository query.
        // Search, branchCode and status filtering still need
        // to be implemented in the repository query.
        return employeeRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployeeStatus(Long id, String newStatus) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

        try {
            // Converts the String (e.g., "ACTIVE") to the Enum
            EmploymentStatus statusEnum = EmploymentStatus.valueOf(newStatus.trim().toUpperCase());
            employee.setEmploymentStatus(statusEnum);
            employee.setUpdatedAt(Instant.now());

            Employee updated = employeeRepository.save(employee);
            return mapToResponse(updated);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + newStatus);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployeesByBranch(String branchCode, int page, int size) {
        Page<Employee> createdAt = employeeRepository.findByBranchCode(branchCode, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return createdAt.map(employee -> mapToResponse(employee));
    }


    @Override
    @Transactional(readOnly = true)
    public EmployeeDetails findEmp(String employeeCode) {

        Employee employee = employeeRepository.findByEmployeeCodeIgnoreCase(employeeCode).orElseThrow(() -> new RuntimeException("Employee not found"));

        // 1. Map employee documents
        List<DocumentDto> documents = employee.getDocuments()
                .stream()
                .map(document -> {
                    DocumentDto dto = new DocumentDto();
                    dto.setDocumentType(document.getDocumentType());
                    dto.setDocumentNumber(document.getDocumentNumber());
                    dto.setVerificationStatus(document.getVerificationStatus().toString());
                    return dto;
                }).toList();

        // 2. Map employee bank account
        EmployeeBankAccount bankAccount = employee.getBankAccount();

        BankAccountDto bankAccountDto = null;

        if (bankAccount != null) {
            bankAccountDto = new BankAccountDto();
            bankAccountDto.setAccountNumber(bankAccount.getAccountNumber());
            bankAccountDto.setIfscCode(bankAccount.getIfscCode());
            bankAccountDto.setBankName(bankAccount.getBankName());
            bankAccountDto.setAccountHolderName(bankAccount.getAccountHolderName()
            );
        }

        // 3. Build employee response
        return EmployeeDetails.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .authUserId(employee.getAuthUserId())
                .name(employee.getName())
                .dateOfBirth(employee.getDateOfBirth())
                .gender(employee.getGender() != null
                        ? employee.getGender().name() : null)
                .email(employee.getEmail())
                .mobileNumber(employee.getMobileNumber())

                .jobTitle(employee.getJobTitle())
                .department(employee.getDepartment())
                .grade(employee.getGrade())
                .employeeType(employee.getEmployeeType() != null
                        ? employee.getEmployeeType().name() : null)
                .employmentStatus(employee.getEmploymentStatus() != null
                        ? employee.getEmploymentStatus().name() : null)
                .maritalStatus(employee.getMaritalStatus())

                .branchCode(employee.getBranchCode())
                .branchName(employee.getBranchName())

                .dateOfJoining(employee.getDateOfJoining())
                .documents(documents)
                .bankAccount(bankAccountDto)

                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }


    // ENTITY TO RESPONSE DTO
    private EmployeeResponse mapToResponse(Employee emp) {

        List<DocumentDto> docs = emp.getDocuments() == null ? List.of() : emp.getDocuments().stream().map(document -> {
                            DocumentDto dto = new DocumentDto();
                            dto.setDocumentType(document.getDocumentType());
                            dto.setDocumentNumber(document.getDocumentNumber());;
                            dto.setVerificationStatus(document.getVerificationStatus() != null ? document.getVerificationStatus().name() : null);
                            return dto;
                        })
                        .toList();

        BankAccountDto bankDto = null;

        if (emp.getBankAccount() != null) {

            EmployeeBankAccount bankAccount = emp.getBankAccount();

            bankDto = new BankAccountDto();
            bankDto.setAccountNumber(bankAccount.getAccountNumber());
            bankDto.setIfscCode(bankAccount.getIfscCode());
            bankDto.setBankName(bankAccount.getBankName());
            bankDto.setAccountHolderName(bankAccount.getAccountHolderName());
        }

        return EmployeeResponse.builder()
                .id(emp.getId())
                .employeeCode(emp.getEmployeeCode())
                .authUserId(emp.getAuthUserId())
                .name(emp.getName())
                .dateOfBirth(emp.getDateOfBirth())
                .gender(emp.getGender().name())
                .maritalStatus(emp.getMaritalStatus())
                .email(emp.getEmail())
                .mobileNumber(emp.getMobileNumber())
                .jobTitle(emp.getJobTitle())
                .department(emp.getDepartment())
                .grade(emp.getGrade())
                .employeeType(emp.getEmployeeType().name())
                .employmentStatus(emp.getEmploymentStatus().name())
                .branchCode(emp.getBranchCode())
                .branchName(emp.getBranchName())
                .dateOfJoining(emp.getDateOfJoining())
                .documents(docs)
                .bankAccount(bankDto)
                .createdAt(emp.getCreatedAt())
                .updatedAt(emp.getUpdatedAt())
                .build();
    }
    private String generateEmployeeCode() {
        String prefix = "NOVA";

        String employeeCode;

        do {
            employeeCode = prefix + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 3)
                    .toUpperCase();
        } while (employeeRepository.existsByEmployeeCode(employeeCode));

        return employeeCode;
    }
    // Keep your existing generateEmployeeCode() implementation
    // here if it already exists in this class.
}
