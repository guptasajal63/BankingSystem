package com.obs.controller;

import com.obs.payload.request.TransferRequest;
import com.obs.payload.response.MessageResponse;
import com.obs.service.TransactionService;
import com.obs.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANKER')")
    public ResponseEntity<?> transferFunds(@Valid @RequestBody TransferRequest transferRequest, Principal principal) {
        transactionService.transferFunds(transferRequest, principal.getName());
        return ResponseEntity.ok(new MessageResponse("Transfer successful!"));
    }
    
    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANKER')")
    public List<Transaction> getTransactionHistory(@PathVariable String accountNumber, Principal principal) {
        return transactionService.getTransactionHistory(accountNumber, principal.getName());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('BANKER')")
    public List<Transaction> getPendingTransactions() {
        return transactionService.getPendingTransactions();
    }

    @PutMapping("/{transactionId}/approve")
    @PreAuthorize("hasRole('BANKER')")
    public ResponseEntity<?> approveTransaction(@PathVariable Long transactionId) {
        transactionService.approveTransaction(transactionId);
        return ResponseEntity.ok(new MessageResponse("Transaction approved successfully!"));
    }

    @PutMapping("/{transactionId}/reject")
    @PreAuthorize("hasRole('BANKER')")
    public ResponseEntity<?> rejectTransaction(@PathVariable Long transactionId) {
        transactionService.rejectTransaction(transactionId);
        return ResponseEntity.ok(new MessageResponse("Transaction rejected successfully!"));
    }
    @Autowired
    private com.obs.service.PdfGenerationService pdfGenerationService;

    @GetMapping("/{transactionId}/invoice")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANKER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);
        byte[] pdfBytes = pdfGenerationService.generateTransactionInvoice(transaction);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + transactionId + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
    @Autowired
    private com.obs.repository.AccountRepository accountRepository;

    @GetMapping("/{accountNumber}/statement")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANKER')")
    public ResponseEntity<byte[]> downloadStatement(@PathVariable String accountNumber, Principal principal) {
        com.obs.entity.Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        // Verify ownership (if not banker)
        // If user is banker, they might use the banker endpoint, but if they use this one, allow it (or block if handled by banker controller exclusively).
        // Since @PreAuthorize allows BANKER, we should check if username matches OR if role is BANKER.
        // For simplicity, let's assume this is primarily for the customer self-service.
        if (!account.getUser().getUsername().equals(principal.getName())) {
             // throw exception or return 403
             // Check if user is banker? No easy way to check role from Principal without casting.
             // simpler to treat this as customer-facing. Bankers have their own controller.
             // If a banker tries to access this for another user, it will fail this check.
             throw new IllegalArgumentException("Unauthorized access to account statement");
        }

        List<Transaction> transactions = transactionService.getTransactionHistory(accountNumber, principal.getName());
        byte[] pdfBytes = pdfGenerationService.generateAccountStatement(account, transactions);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement_" + accountNumber + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
