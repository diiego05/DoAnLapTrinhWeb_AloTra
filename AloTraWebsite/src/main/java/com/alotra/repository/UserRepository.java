package com.alotra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}