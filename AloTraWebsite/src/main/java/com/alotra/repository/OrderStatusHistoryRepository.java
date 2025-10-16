package com.alotra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alotra.entity.OrderStatusHistory;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(Long orderId);

}

