package com.alotra.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.alotra.entity.Branch;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    // 🔍 Tìm kiếm theo tên hoặc địa chỉ
    List<Branch> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(String name, String address);

    // 🏪 Lọc theo trạng thái
    List<Branch> findByStatus(String status);

    // 🔍 Tìm kiếm + lọc trạng thái kết hợp
    List<Branch> findByNameContainingIgnoreCaseAndStatusOrAddressContainingIgnoreCaseAndStatus(
            String name, String status1, String address, String status2);

    // 📌 Tìm kiếm nâng cao (tên hoặc địa chỉ, có thể null)
    @Query("SELECT b FROM Branch b WHERE " +
            "(:keyword IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.address) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR b.status = :status)")
    List<Branch> searchAndFilter(String keyword, String status);

    boolean existsByName(String name);

    // 📍 ✅ Lấy chi nhánh mà vendor đang quản lý
    Optional<Branch> findByManagerId(Long managerId);

    // 📍 ✅ Kiểm tra chi nhánh có thuộc về vendor không
    boolean existsByIdAndManagerId(Long branchId, Long managerId);

    // 🌐 Coordinate-aware queries (optional helpers)
    List<Branch> findByLatitudeIsNotNullAndLongitudeIsNotNull();
    List<Branch> findByStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(String status);

    // 🆕 Branches missing coordinates
    List<Branch> findByLatitudeIsNullOrLongitudeIsNull();
}