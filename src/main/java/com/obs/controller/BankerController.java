package com.obs.controller;

import com.obs.entity.Transaction;
import com.obs.service.TransactionService;
import com.obs.payload.response.MessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/banker")
public class BankerController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/pending-transactions")
    @PreAuthorize("hasRole('BANKER') or hasRole('ADMIN')")
    public List<Transaction> getPendingTransactions() {
        return transactionService.getPendingTransactions();
    }

    @PutMapping("/transactions/{id}/approve")
    @PreAuthorize("hasRole('BANKER') or hasRole('ADMIN')")
    public ResponseEntity<?> approveTransaction(@PathVariable Long id) {
        transactionService.approveTransaction(id);
        return ResponseEntity.ok(new MessageResponse("Transaction approved successfully"));
    }

    @PutMapping("/transactions/{id}/reject")
    @PreAuthorize("hasRole('BANKER') or hasRole('ADMIN')")
    public ResponseEntity<?> rejectTransaction(@PathVariable Long id) {
        transactionService.rejectTransaction(id);
        return ResponseEntity.ok(new MessageResponse("Transaction rejected successfully"));
    }
    @Autowired
    private com.obs.repository.AccountRepository accountRepository;

    @Autowired
    private com.obs.service.PdfGenerationService pdfGenerationService;

    @GetMapping("/accounts/{accountNumber}/transactions")
    @PreAuthorize("hasRole('BANKER') or hasRole('ADMIN')")
    public List<Transaction> getAccountTransactions(@PathVariable String accountNumber) {
        return transactionService.getTransactionsForAccount(accountNumber);
    }

    @GetMapping("/accounts/{accountNumber}/statement")
    @PreAuthorize("hasRole('BANKER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadAccountStatement(@PathVariable String accountNumber) {
        com.obs.entity.Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        List<Transaction> transactions = transactionService.getTransactionsForAccount(accountNumber);
        
        byte[] pdfBytes = pdfGenerationService.generateAccountStatement(account, transactions);
        
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement_" + accountNumber + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
