package com.obs.service;

import com.obs.entity.Account;
import com.obs.entity.RecurringPayment;
import com.obs.repository.AccountRepository;
import com.obs.repository.RecurringPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecurringPaymentService {

    @Autowired
    private RecurringPaymentRepository recurringPaymentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionService transactionService;

    public RecurringPayment createRecurringPayment(String accountNumber, BigDecimal amount, String targetAccountNumber, String frequency, LocalDate startDate, LocalDate endDate) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        RecurringPayment payment = new RecurringPayment();
        payment.setAccount(account);
        payment.setAmount(amount);
        payment.setTargetAccountNumber(targetAccountNumber);
        payment.setFrequency(frequency);
        payment.setStartDate(startDate);
        payment.setEndDate(endDate);
        payment.setNextPaymentDate(startDate);
        payment.setStatus("ACTIVE");
        payment.setCreatedAt(LocalDateTime.now());

        return recurringPaymentRepository.save(payment);
    }

    public List<RecurringPayment> getRecurringPayments(String username) {
        // In a real app we'd filter by username more strictly via Repo, but here we can filter by account
         // Or getting all accounts for user and then all payments.
         // Let's assume passed account number in controller or just return empty for simplicty if not managed
         // Impl: Controller will usually pass Account Number or User. 
         // Let's implement finding by Account
         return List.of(); // Placeholder, actual impl in Controller
    }
    
    public List<RecurringPayment> getRecurringPaymentsByAccount(String accountNumber, String username) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        if (!account.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        
        return recurringPaymentRepository.findByAccount(account);
    }

    public void stopRecurringPayment(Long id, String username) {
        RecurringPayment payment = recurringPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!payment.getAccount().getUser().getUsername().equals(username)) {
             throw new IllegalArgumentException("Unauthorized");
        }

        payment.setStatus("STOPPED");
        recurringPaymentRepository.save(payment);
    }

    @Scheduled(cron = "0 0 12 * * ?") // Every day at 12 PM
    @Transactional
    public void processRecurringPayments() {
        LocalDate today = LocalDate.now();
        List<RecurringPayment> payments = recurringPaymentRepository.findByStatusAndNextPaymentDateLessThanEqual("ACTIVE", today);

        for (RecurringPayment payment : payments) {
             try {
                 transactionService.executeRecurringTransfer(payment.getAccount(), payment.getTargetAccountNumber(), payment.getAmount());
                 
                 // Update Next Payment Date
                 LocalDate nextDate = payment.getNextPaymentDate();
                 switch (payment.getFrequency()) {
                     case "DAILY":
                         nextDate = nextDate.plusDays(1);
                         break;
                     case "WEEKLY":
                         nextDate = nextDate.plusWeeks(1);
                         break;
                     case "MONTHLY":
                         nextDate = nextDate.plusMonths(1);
                         break;
                 }
                 
                 payment.setNextPaymentDate(nextDate);
                 
                 // Check End Date
                 if (payment.getEndDate() != null && nextDate.isAfter(payment.getEndDate())) {
                     payment.setStatus("COMPLETED");
                 }
                 
                 recurringPaymentRepository.save(payment);
                 
             } catch (Exception e) {
                 System.err.println("Failed to process recurring payment " + payment.getId() + ": " + e.getMessage());
                 // Optionally disable payment or retry later
             }
        }
    }
}
