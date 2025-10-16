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
    List<ShippingAssignment> findByShipperIdAndStatus(Long shipperId, String status);

    Optional<ShippingAssignment> findByOrderIdAndShipperId(Long orderId, Long shipperId);

    List<ShippingAssignment> findByOrderId(Long orderId);

    @Modifying
    @Query("UPDATE ShippingAssignment s SET s.status = :status WHERE s.orderId = :orderId AND s.shipperId <> :acceptedId")
    void updateOtherAssignments(Long orderId, Long acceptedId, String status);

    List<ShippingAssignment> findByShipperIdAndStatusIn(Long shipperId, List<String> statuses);


}
