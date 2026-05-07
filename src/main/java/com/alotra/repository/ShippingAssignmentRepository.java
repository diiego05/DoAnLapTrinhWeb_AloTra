package com.alotra.repository;

import com.alotra.entity.ShippingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingAssignmentRepository extends JpaRepository<ShippingAssignment, Long> {

    // 🔸 Lấy tất cả đơn theo shipper (bất kể trạng thái)
    List<ShippingAssignment> findByShipperId(Long shipperId);

    // 🔸 Lấy theo shipper + trạng thái cụ thể
    List<ShippingAssignment> findByShipperIdAndStatus(Long shipperId, String status);

    // 🔸 Lấy theo shipper + nhiều trạng thái
    List<ShippingAssignment> findByShipperIdAndStatusIn(Long shipperId, List<String> statuses);

    // 🔸 Tìm 1 assignment duy nhất theo đơn + shipper
    Optional<ShippingAssignment> findByOrderIdAndShipperId(Long orderId, Long shipperId);

    // 🔸 Tìm tất cả assignment của 1 đơn
    List<ShippingAssignment> findByOrderId(Long orderId);

    // 🔸 Cập nhật trạng thái tất cả shipper khác khi có người nhận đơn
    @Modifying
    @Query("UPDATE ShippingAssignment s SET s.status = :status WHERE s.orderId = :orderId AND s.shipperId <> :acceptedId")
    void updateOtherAssignments(Long orderId, Long acceptedId, String status);
}
