package com.alotra.repository;

import com.alotra.entity.OrderItemTopping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemToppingRepository extends JpaRepository<OrderItemTopping, Long> {
}
