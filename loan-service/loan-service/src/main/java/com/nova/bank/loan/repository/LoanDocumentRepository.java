package com.nova.bank.loan.repository;

import com.nova.bank.loan.entity.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, String> {
    List<LoanDocument> findByApplicationId(String applicationId);
}
