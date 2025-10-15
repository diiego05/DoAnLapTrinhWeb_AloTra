// com/alotra/repository/OrderItemRepository.java
package com.alotra.repository;

import com.alotra.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
