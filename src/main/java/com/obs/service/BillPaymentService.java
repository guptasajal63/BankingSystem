package com.obs.service;

import com.obs.entity.Account;
import com.obs.entity.BillPayment;
import com.obs.entity.User;
import com.obs.repository.AccountRepository;
import com.obs.repository.BillPaymentRepository;
import com.obs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillPaymentService {

    @Autowired
    private BillPaymentRepository billPaymentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void payBill(Long userId, String fromAccountNumber, String billerName, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Account account = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to user");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        // Deduct balance
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        // Record Bill Payment
        BillPayment billPayment = new BillPayment();
        billPayment.setBillerName(billerName);
        billPayment.setAmount(amount);
        billPayment.setDueDate(LocalDateTime.now()); // Assuming immediate payment
        billPayment.setStatus("PAID");
        billPayment.setUser(user);

        billPaymentRepository.save(billPayment);
    }

    public List<BillPayment> getMyBills(Long userId) {
        return billPaymentRepository.findByUserId(userId);
    }
}
