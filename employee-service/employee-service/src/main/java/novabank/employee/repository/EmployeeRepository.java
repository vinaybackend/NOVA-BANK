package novabank.employee.repository;

import novabank.employee.entities.Employee;
import novabank.employee.entities.enums.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);
    boolean existsByEmailIgnoreCase(String email);

    Optional<Employee> findByEmployeeCodeIgnoreCase(String employeeCode);
    Optional<Employee> findByEmailIgnoreCase(String email);

    // Fixed to match your String branchCode
    Page<Employee> findByBranchCode(String branchCode, Pageable pageable);

    Page<Employee> findByEmploymentStatus(EmploymentStatus status, Pageable pageable);

    boolean existsByEmployeeCode(String employeeCode);
}