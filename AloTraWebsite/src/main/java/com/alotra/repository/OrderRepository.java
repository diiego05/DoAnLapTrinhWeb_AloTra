// com/alotra/repository/OrderRepository.java
package com.alotra.repository;

import com.alotra.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
