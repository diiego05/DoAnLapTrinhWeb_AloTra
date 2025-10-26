// com/alotra/repository/OrderRepository.java
package com.alotra.repository;

import com.alotra.entity.Order;
import com.alotra.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByCode(String code);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByOrderByCreatedAtDesc();

    // ✅ Admin - lọc theo branch
    List<Order> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    // ✅ Admin - lọc theo status
    List<Order> findByStatusOrderByCreatedAtDesc(String status);

    // ✅ Admin - lọc theo branch + status
    List<Order> findByBranchIdAndStatusOrderByCreatedAtDesc(Long branchId, String status);

    // ============================
    // 📦 VENDOR TRUY VẤN ĐƠN HÀNG
    // ============================
    @Query("""
        SELECT o FROM Order o
        JOIN Branch b ON o.branchId = b.id
        WHERE b.manager.id = :vendorId
        ORDER BY o.createdAt DESC
    """)
    List<Order> findOrdersByVendorId(@Param("vendorId") Long vendorId);

    @Query("""
        SELECT o FROM Order o
        JOIN Branch b ON o.branchId = b.id
        WHERE b.manager.id = :vendorId AND o.status = :status
        ORDER BY o.createdAt DESC
    """)
    List<Order> findOrdersByVendorIdAndStatus(@Param("vendorId") Long vendorId,
                                              @Param("status") String status);
}
