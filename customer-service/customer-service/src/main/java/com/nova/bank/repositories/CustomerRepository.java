package com.nova.bank.repositories;

import com.nova.bank.entities.Customer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Page<Customer> findByCustomerIdIn(List<String> customerIds, Pageable pageable);

    Optional<Customer> findByCustomerId(String customerId);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByCustomerId(String customerId);

    Page<Customer> findByBranchCode(String branchCode,Pageable pageable);
}