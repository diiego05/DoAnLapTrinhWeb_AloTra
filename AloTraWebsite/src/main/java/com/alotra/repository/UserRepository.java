package com.alotra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.alotra.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR u.fullName LIKE %:keyword% OR u.email LIKE %:keyword%) AND " +
           "(:roleId IS NULL OR u.role.id = :roleId) AND " +
           "(:status IS NULL OR u.status = :status)")
    List<User> searchAndFilter(String keyword, Long roleId, String status);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByIdCardNumber(String idCardNumber);
    long countByEmailContaining(String emailPattern);
<<<<<<< HEAD

    @Query("""
            SELECT COUNT(DISTINCT u.id)
            FROM User u
            JOIN Order o ON u.id = o.userId
            WHERE o.branchId = :branchId
        """)
        long countDistinctByBranch(@Param("branchId") Long branchId);
=======
>>>>>>> 993ad7d594830db625080aef2c3d6c114852b0f3
}