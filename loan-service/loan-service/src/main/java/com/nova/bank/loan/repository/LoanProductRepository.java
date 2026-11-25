package com.nova.bank.loan.repository;

import com.nova.bank.loan.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    List<LoanProduct> findByActiveTrue();
}
