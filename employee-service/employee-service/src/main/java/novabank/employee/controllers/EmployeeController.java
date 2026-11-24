package novabank.employee.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import novabank.employee.dto.request.CreateEmployeeRequest;
import novabank.employee.dto.response.EmployeeDetails;
import novabank.employee.dto.response.EmployeeResponse;
import novabank.employee.services.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;


    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'BRANCH_MANAGER')")
    public ResponseEntity<EmployeeResponse> registerEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.registerEmployee(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','BRANCH_MANAGER')")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'BRANCH_MANAGER')")
    public ResponseEntity<Page<EmployeeResponse>> getAllEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(employeeService.getAllEmployees(search, branchCode, status, pageable));
    }


    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'BRANCH_MANAGER')")
    public ResponseEntity<EmployeeResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(employeeService.updateEmployeeStatus(id, status));
    }
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @GetMapping("/all-employee/{branchCode}")
    public ResponseEntity<Page<EmployeeResponse>> allEmpByBranchCode(@PathVariable("branchCode") String branchCode,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size){
        Page<EmployeeResponse> allEmployeesByBranch = employeeService.getAllEmployeesByBranch(branchCode, page, size);
        return new ResponseEntity<>(allEmployeesByBranch,HttpStatus.OK);

    }
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @GetMapping("/details/{employeeCode}")
    public ResponseEntity<EmployeeDetails> empDetails(@PathVariable("employeeCode") String employeeCode){
        EmployeeDetails emp = employeeService.findEmp(employeeCode);
        return new ResponseEntity<>(emp,HttpStatus.OK);
    }
}