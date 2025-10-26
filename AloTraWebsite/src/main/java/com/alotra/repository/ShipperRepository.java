package com.alotra.repository;

import com.alotra.entity.Shipper;
import com.alotra.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ShipperRepository extends JpaRepository<Shipper, Long> {
    Optional<Shipper> findByUser(User user);
    List<Shipper> findByStatus(String status);
    Optional<Shipper> findByUserId(Long userId);
    boolean existsByUserIdAndStatus(Long userId, String status);


    @Query("SELECT s FROM Shipper s " +
           "WHERE (:status IS NULL OR s.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(s.ward) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(s.district) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Shipper> searchShippers(String status, String keyword);
    List<Shipper> findByCarrierIdAndIsDeletedFalse(Long carrierId);
    Optional<Shipper> findByUser_Id(Long userId);
    @Query("""
            SELECT s.id
            FROM Shipper s
            WHERE s.user.id = :userId
            AND s.status = 'APPROVED'
        """)
        Long findIdByUserId(@Param("userId") Long userId);

}
