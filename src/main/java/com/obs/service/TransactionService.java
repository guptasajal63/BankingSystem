package com.obs.service;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.Transaction;
import com.obs.payload.request.TransferRequest;
import com.obs.repository.AccountRepository;
import com.obs.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public String transferFunds(TransferRequest transferRequest, String username) {
        Account fromAccount = accountRepository.findByAccountNumber(transferRequest.getFromAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));

        if (!fromAccount.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("You do not own the source account");
        }

        Account toAccount = accountRepository.findByAccountNumber(transferRequest.getToAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Target account not found"));

        if (!fromAccount.isActive()) {
            throw new IllegalArgumentException("Source account is frozen/inactive");
        }
        
        if (!toAccount.isActive()) {
            throw new IllegalArgumentException("Target account is frozen/inactive");
        }

        if (fromAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        BigDecimal transferLimit = new BigDecimal("10000");

        if (transferRequest.getAmount().compareTo(transferLimit) > 0 && fromAccount.getAccountType() != AccountType.CURRENT) {
            // High value transaction - HOLD funds and mark PENDING
             // Deduct from source immediately to lock funds (or move to reserved balance, but keeping simple here)
            fromAccount.setBalance(fromAccount.getBalance().subtract(transferRequest.getAmount()));
            accountRepository.save(fromAccount);
            
            Transaction transaction = new Transaction();
            transaction.setAccount(fromAccount);
            transaction.setAmount(transferRequest.getAmount().negate());
            transaction.setType("DEBIT");
            transaction.setTimestamp(LocalDateTime.now());
            transaction.setTargetAccountNumber(toAccount.getAccountNumber());
            transaction.setDescription("Transfer to " + toAccount.getAccountNumber() + " (PENDING APPROVAL)");
            transaction.setStatus("PENDING");
            transactionRepository.save(transaction);

            return "PENDING";
        } else {
            // Automatic Approval
            fromAccount.setBalance(fromAccount.getBalance().subtract(transferRequest.getAmount()));
            accountRepository.save(fromAccount);

            toAccount.setBalance(toAccount.getBalance().add(transferRequest.getAmount()));
            accountRepository.save(toAccount);

            // Debit Record
            Transaction debitTransaction = new Transaction();
            debitTransaction.setAccount(fromAccount);
            debitTransaction.setAmount(transferRequest.getAmount().negate());
            debitTransaction.setType("DEBIT");
            debitTransaction.setTimestamp(LocalDateTime.now());
            debitTransaction.setTargetAccountNumber(toAccount.getAccountNumber());
            debitTransaction.setDescription("Transfer to " +  toAccount.getUser().getUsername());
            debitTransaction.setStatus("SUCCESS");
            transactionRepository.save(debitTransaction);

            // Credit Record
            Transaction creditTransaction = new Transaction();
            creditTransaction.setAccount(toAccount);
            creditTransaction.setAmount(transferRequest.getAmount());
            creditTransaction.setType("CREDIT");
            creditTransaction.setTimestamp(LocalDateTime.now());
            creditTransaction.setTargetAccountNumber(fromAccount.getAccountNumber());
            creditTransaction.setDescription("Received from " + fromAccount.getUser().getUsername());
            creditTransaction.setStatus("SUCCESS");
            transactionRepository.save(creditTransaction);

            return "SUCCESS";
        }
    }
    
    @Transactional
    public void approveTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        if (!"PENDING".equals(transaction.getStatus())) {
             throw new IllegalArgumentException("Transaction is not pending");
        }
        
        Account fromAccount = transaction.getAccount();
        Account toAccount = accountRepository.findByAccountNumber(transaction.getTargetAccountNumber())
                 .orElseThrow(() -> new IllegalArgumentException("Target account not found"));
        
        if (!toAccount.isActive()) {
             throw new IllegalArgumentException("Target account is frozen/inactive");
        }
        
        // Funds were already deducted from source. Now add to target.
        toAccount.setBalance(toAccount.getBalance().add(transaction.getAmount().abs()));
        accountRepository.save(toAccount);
        
        transaction.setStatus("SUCCESS");
        transaction.setDescription(transaction.getDescription().replace(" (PENDING APPROVAL)", ""));
        transactionRepository.save(transaction);
        
        // Create Credit Record for Receiver
        Transaction creditTransaction = new Transaction();
        creditTransaction.setAccount(toAccount);
        creditTransaction.setAmount(transaction.getAmount().abs());
        creditTransaction.setType("CREDIT");
        creditTransaction.setTimestamp(LocalDateTime.now());
        creditTransaction.setTargetAccountNumber(fromAccount.getAccountNumber());
        creditTransaction.setDescription("Received from " + fromAccount.getUser().getUsername());
        creditTransaction.setStatus("SUCCESS");
        transactionRepository.save(creditTransaction);
    }

    @Transactional
    public void rejectTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        if (!"PENDING".equals(transaction.getStatus())) {
             throw new IllegalArgumentException("Transaction is not pending");
        }
        
        Account fromAccount = transaction.getAccount();
        
        // Refund source account
        fromAccount.setBalance(fromAccount.getBalance().add(transaction.getAmount().abs()));
        accountRepository.save(fromAccount);
        
        transaction.setStatus("REJECTED");
        transactionRepository.save(transaction);
    }

    public List<Transaction> getPendingTransactions() {
        // This is a simplified way. Ideally we should have a custom query.
        // Since we don't have a status based query in repo, let's filter all or add method to repo.
        // Let's add method to repo first (implicit step in next tool call? or just filter here if list is small, better to add to repo)
        // I'll add to Repo in next step, for now I will assume it exists or use findAll and filter
        return transactionRepository.findAll().stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .toList();
    }
    
    public List<Transaction> getTransactionHistory(String accountNumber, String username) {
         Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
         
         if (!account.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized access to account history");
        }
         
         return transactionRepository.findByAccount(account);
    }
    
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    @Transactional
    public void executeRecurringTransfer(Account fromAccount, String targetAccountNumber, BigDecimal amount) {
        Account toAccount = accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Target account not found"));

        if (!fromAccount.isActive()) {
            throw new IllegalArgumentException("Source account is frozen/inactive");
        }

        if (!toAccount.isActive()) {
            throw new IllegalArgumentException("Target account is frozen/inactive");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);

        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        Transaction debitTransaction = new Transaction();
        debitTransaction.setAccount(fromAccount);
        debitTransaction.setAmount(amount.negate());
        debitTransaction.setType("DEBIT");
        debitTransaction.setTimestamp(LocalDateTime.now());
        debitTransaction.setTargetAccountNumber(toAccount.getAccountNumber());
        debitTransaction.setDescription("Recurring Transfer to " + toAccount.getAccountNumber());
        debitTransaction.setStatus("SUCCESS");
        transactionRepository.save(debitTransaction);

        Transaction creditTransaction = new Transaction();
        creditTransaction.setAccount(toAccount);
        creditTransaction.setAmount(amount);
        creditTransaction.setType("CREDIT");
        creditTransaction.setTimestamp(LocalDateTime.now());
        creditTransaction.setTargetAccountNumber(fromAccount.getAccountNumber());
        creditTransaction.setDescription("Recurring Received from " + fromAccount.getUser().getUsername());
        creditTransaction.setStatus("SUCCESS");
        transactionRepository.save(creditTransaction);
    }
    public List<Transaction> getTransactionsForAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return transactionRepository.findByAccount(account);
    }
}
