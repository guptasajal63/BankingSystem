package com.obs.controller;

import com.obs.entity.RecurringPayment;
import com.obs.payload.request.RecurringPaymentRequest;
import com.obs.payload.response.MessageResponse;
import com.obs.service.RecurringPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/recurring")
public class RecurringPaymentController {

    @Autowired
    private RecurringPaymentService recurringPaymentService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createRecurringPayment(@Valid @RequestBody RecurringPaymentRequest request) {
        recurringPaymentService.createRecurringPayment(
                request.getFromAccountNumber(),
                request.getAmount(),
                request.getTargetAccountNumber(),
                request.getFrequency(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ResponseEntity.ok(new MessageResponse("Recurring payment scheduled successfully!"));
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<RecurringPayment> getRecurringPayments(@PathVariable String accountNumber, Principal principal) {
        return recurringPaymentService.getRecurringPaymentsByAccount(accountNumber, principal.getName());
    }

    @PutMapping("/{id}/stop")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> stopRecurringPayment(@PathVariable Long id, Principal principal) {
        recurringPaymentService.stopRecurringPayment(id, principal.getName());
        return ResponseEntity.ok(new MessageResponse("Recurring payment stopped successfully!"));
    }
}
