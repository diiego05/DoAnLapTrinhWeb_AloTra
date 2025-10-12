package com.alotra.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.alotra.entity.Branch;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    /**
     * Tìm kiếm chi nhánh có Tên HOẶC Địa chỉ chứa từ khóa (không phân biệt hoa/thường).
     * @param name Từ khóa tìm kiếm cho tên
     * @param address Từ khóa tìm kiếm cho địa chỉ
     * @return Danh sách chi nhánh phù hợp
     */
    List<Branch> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(String name, String address);

    /**
     * Lọc chi nhánh theo Trạng thái.
     * @param status Trạng thái cần lọc ('ACTIVE' hoặc 'TEMPORARILY_CLOSED')
     * @return Danh sách chi nhánh phù hợp
     */
    List<Branch> findByStatus(String status);

    /**
     * Tìm kiếm kết hợp: (Tên HOẶC Địa chỉ chứa từ khóa) VÀ có Trạng thái phù hợp.
     * @param name Từ khóa tìm kiếm cho tên
     * @param status1 Trạng thái
     * @param address Từ khóa tìm kiếm cho địa chỉ
     * @param status2 Trạng thái (lặp lại vì có 2 điều kiện OR)
     * @return Danh sách chi nhánh phù hợp
     */
    List<Branch> findByNameContainingIgnoreCaseAndStatusOrAddressContainingIgnoreCaseAndStatus(String name, String status1, String address, String status2);

    @Query("SELECT b FROM Branch b WHERE " +
            "(:keyword IS NULL OR b.name LIKE %:keyword% OR b.address LIKE %:keyword%) AND " +
            "(:status IS NULL OR b.status = :status)")
     List<Branch> searchAndFilter(String keyword, String status);

    boolean existsByName(String name);
}