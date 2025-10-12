package com.alotra.repository;

import com.alotra.entity.request.BranchRegistrationRequest;
import com.alotra.entity.request.BranchRequestType;
import com.alotra.entity.request.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BranchRegistrationRequestRepository extends JpaRepository<BranchRegistrationRequest, Long> {
    boolean existsByUserIdAndTypeAndStatus(Long userId, BranchRequestType type, RequestStatus status);
    List<BranchRegistrationRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndStatus(Long userId, RequestStatus status);

    List<BranchRegistrationRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);
    List<BranchRegistrationRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    @Query("SELECT b FROM BranchRegistrationRequest b " +
           "WHERE (:status IS NULL OR b.status = :status) " +
           "AND (:type IS NULL OR b.type = :type) " +
           "AND (:keyword IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<BranchRegistrationRequest> searchRequests(RequestStatus status,
                                                   com.alotra.entity.request.BranchRequestType type,
                                                   String keyword);
}
