package com.alotra.repository;

import com.alotra.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Lấy các danh mục cấp 1 (không có parent) sắp xếp theo sortOrder
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.sortOrder ASC")
    List<Category> findTopLevelCategories();
    
    /**
     * 🗺️ Lấy TẤT CẢ categories sắp xếp theo sortOrder (dùng cho Dynamic Menu)
     */
    @Query("SELECT c FROM Category c ORDER BY c.sortOrder ASC")
    List<Category> findAllOrderBySortOrderAsc();
}