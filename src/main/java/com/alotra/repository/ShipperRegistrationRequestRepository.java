package com.alotra.repository;

import com.alotra.entity.request.ShipperRegistrationRequest;
import com.alotra.entity.request.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipperRegistrationRequestRepository extends JpaRepository<ShipperRegistrationRequest, Long> {
    boolean existsByUserIdAndStatus(Long userId, RequestStatus status);
    List<ShipperRegistrationRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
