package com.alotra.repository;

import com.alotra.entity.Address;
import com.alotra.entity.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
    Optional<Address> findByUserAndIsDefaultTrue(User user);
    List<Address> findByUser_IdOrderByIsDefaultDesc(Long userId);

    Optional<Address> findByIdAndUser_Id(Long id, Long userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultForUser(Long userId);
}