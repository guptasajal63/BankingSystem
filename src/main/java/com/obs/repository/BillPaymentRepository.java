package com.obs.repository;

import com.obs.entity.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {
    List<BillPayment> findByUserId(Long userId);
}
