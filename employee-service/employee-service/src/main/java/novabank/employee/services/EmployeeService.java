package novabank.employee.services;

import novabank.employee.dto.request.CreateEmployeeRequest;
import novabank.employee.dto.response.EmployeeDetails;
import novabank.employee.dto.response.EmployeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {
    EmployeeResponse registerEmployee(CreateEmployeeRequest request);
    EmployeeResponse getEmployeeById(Long id);
    EmployeeResponse getEmployeeByCode(String employeeCode);
    Page<EmployeeResponse> getAllEmployees(String search, String branchCode, String status, Pageable pageable);
    EmployeeResponse updateEmployeeStatus(Long id, String newStatus);

    Page<EmployeeResponse> getAllEmployeesByBranch(String branchCode, int page, int size);

    EmployeeDetails findEmp(String employeeCode);
}