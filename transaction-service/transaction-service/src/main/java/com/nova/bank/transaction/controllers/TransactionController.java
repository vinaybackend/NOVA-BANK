package com.nova.bank.transaction.controllers;

import com.nova.bank.transaction.dto.CreateDepositRequest;
import com.nova.bank.transaction.dto.CreateWithdrawalRequest;
import com.nova.bank.transaction.dto.TransactionResponse;
import com.nova.bank.transaction.services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {

        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody CreateDepositRequest request) {

        TransactionResponse response = transactionService.deposit(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody CreateWithdrawalRequest request) {

        TransactionResponse response = transactionService.withdraw(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}