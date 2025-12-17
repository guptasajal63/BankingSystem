package com.obs.repository;

import com.obs.entity.Account;
import com.obs.entity.RecurringPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, Long> {
    List<RecurringPayment> findByAccount(Account account);
    List<RecurringPayment> findByStatusAndNextPaymentDateLessThanEqual(String status, LocalDate date);
}
