package com.obs.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import com.obs.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.Transaction;
import com.obs.entity.User;
import com.obs.payload.response.AccountDetailsResponse;
import com.obs.payload.response.MessageResponse;
import com.obs.repository.AccountRepository;
import com.obs.repository.UserRepository;


@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @GetMapping("/my-accounts")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANKER') or hasRole('ADMIN')")
    public List<Account> getMyAccounts(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).get();
        return accountRepository.findByUser(user);
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createAccount(@RequestBody com.obs.payload.request.CreateAccountRequest request, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).get();

        Account account = new Account();
        account.setAccountType(request.getAccountType());
        account.setBalance(BigDecimal.ZERO);
        account.setUser(user);
        account.setAccountNumber(generateAccountNumber());
        account.setPanCardNumber(request.getPanCardNumber());

        if (request.getAccountType() == AccountType.CURRENT) {
            if (request.getBusinessName() == null || request.getBusinessName().isEmpty() ||
                    request.getBusinessAddress() == null || request.getBusinessAddress().isEmpty()) {
                throw new IllegalArgumentException("Business Name and Address are required for Current Account.");
            }
            account.setBusinessName(request.getBusinessName());
            account.setBusinessAddress(request.getBusinessAddress());
        }

        accountRepository.save(account);

        return ResponseEntity.ok(new MessageResponse("Account created successfully!"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('BANKER')")
    public ResponseEntity<?> searchAccount(@RequestParam String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new com.obs.exception.ResourceNotFoundException("Account not found"));

        AccountDetailsResponse response = new AccountDetailsResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.isActive(),
                account.getUser().getUsername(),
                account.getUser().getId(),
                account.getUser().getEmail()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountNumber}/toggle-active")
    @PreAuthorize("hasRole('BANKER')")
    public ResponseEntity<?> toggleAccountActive(@PathVariable String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new com.obs.exception.ResourceNotFoundException("Account not found"));

        account.setActive(!account.isActive());
        accountRepository.save(account);

        return ResponseEntity.ok(new MessageResponse("Account status updated to " + (account.isActive() ? "Active" : "Frozen")));
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('BANKER')")
    public ResponseEntity<?> deposit(@RequestBody java.util.Map<String, Object> request) {
        String accountNumber = (String) request.get("accountNumber");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new com.obs.exception.ResourceNotFoundException("Account not found"));

        if (!account.isActive()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Cannot deposit to frozen/inactive account"));
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // Record Transaction
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount); // Positive for Credit
        transaction.setType("CREDIT");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setDescription("Cash Deposit by Banker");
        transaction.setStatus("SUCCESS");
        // targetAccountNumber is null for cash deposit

        transactionRepository.save(transaction);

        return ResponseEntity.ok(new MessageResponse("Signal deposit successful. New balance: " + account.getBalance()));
    }

    private String generateAccountNumber() {
        Random rand = new Random();
        String card = "1000";
        for (int i = 0; i < 12; i++)
        {
            int n = rand.nextInt(10) + 0;
            card += Integer.toString(n);
        }
        return card;
    }
}
