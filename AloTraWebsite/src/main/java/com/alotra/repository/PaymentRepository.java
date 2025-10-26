package com.alotra.repository;

import com.alotra.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionCode(String code);
    List<Payment> findByOrderId(Long orderId);
    Optional<Payment> findTopByOrderIdOrderByPaidAtDesc(Long orderId);
    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);


}
