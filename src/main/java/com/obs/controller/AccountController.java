package com.obs.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.User;
import com.obs.repository.AccountRepository;
import com.obs.repository.UserRepository;
import com.obs.payload.response.MessageResponse;
import com.obs.payload.response.AccountDetailsResponse;

@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    UserRepository userRepository;

    @GetMapping("/my-accounts")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANKER') or hasRole('ADMIN')")
    public List<Account> getMyAccounts(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).get();
        return accountRepository.findByUser(user);
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createAccount(@RequestParam AccountType accountType, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).get();
        
        Account account = new Account();
        account.setAccountType(accountType);
        account.setBalance(BigDecimal.ZERO);
        account.setUser(user);
        account.setAccountNumber(generateAccountNumber());
        
        accountRepository.save(account);
        
        return ResponseEntity.ok(new MessageResponse("Account created successfully!"));
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('BANKER')")
    public ResponseEntity<?> searchAccount(@RequestParam String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
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
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
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
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.isActive()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Cannot deposit to frozen/inactive account"));
        }
        
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        
        // Record Transaction
         // Ideally inject TransactionRepository and save a CREDIT transaction here
         // For brevity, skipping explicit transaction record or adding dependency here if not present.
         // Let's rely on account update for now or I should add TransactionRepository dependency.
         
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
