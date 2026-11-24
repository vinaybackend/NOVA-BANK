package com.nova.bank.transaction.controllers;
import com.nova.bank.transaction.dto.*;
import com.nova.bank.transaction.services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','BRANCH_MANAGER')")
    @GetMapping("/mine")
    public ResponseEntity<List<TransactionResponse>> getMyRecentTransactions() {
        return ResponseEntity.ok(transactionService.getMyRecentTransactions());
    }
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','BRANCH_MANAGER')")
    @GetMapping("/admin")
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(transactionService.getAllTransactions(page, size));
    }

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','BRANCH_MANAGER')")
    @PostMapping("/mine/transfer")
    public ResponseEntity<TransferTransactionResponse> transferForCurrentCustomer(@Valid @RequestBody CreateTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transferForCurrentCustomer(request));
    }


    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','BRANCH_MANAGER')")
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody CreateWithdrawalRequest request) {

        TransactionResponse response = transactionService.withdraw(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ADD THIS
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','BRANCH_MANAGER')")
    @PostMapping("/transfer")
    public ResponseEntity<TransferTransactionResponse> transfer(@Valid @RequestBody CreateTransferRequest request) {

        TransferTransactionResponse transfer = transactionService.transfer(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    @GetMapping("/all-transaction/{branchCode}")
    public ResponseEntity<Page<TransactionResponse>> allTranBranch(@PathVariable("branchCode") String branchCode,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size){
        Page<TransactionResponse> alLTransBranch = transactionService.findAlLTransBranch(branchCode, page, size);
        return new ResponseEntity<>(alLTransBranch,HttpStatus.OK);
    }


}