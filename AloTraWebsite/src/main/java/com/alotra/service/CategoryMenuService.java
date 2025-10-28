package com.alotra.service;

import com.alotra.entity.Category;
import com.alotra.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryMenuService {

    private final CategoryRepository categoryRepository;

    /**
     * 🗺️ Xây dựng menu từ Categories
     * - Lấy tất cả categories
     * - Sắp xếp theo sortOrder
     * - Tạo cấu trúc parent-child
     */
    @Transactional(readOnly = true)
    public List<CategoryMenuDTO> buildMenu() {
        // Lấy tất cả categories sắp xếp theo sortOrder
        List<Category> allCategories = categoryRepository.findAllOrderBySortOrderAsc();

        // Lọc ra các categories cấp cao nhất (không có parent)
        List<Category> topLevelCategories = allCategories.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.toList());

        // Chuyển đổi sang DTO với children
        return topLevelCategories.stream()
                .map(c -> convertToDTO(c, allCategories))
                .collect(Collectors.toList());
    }

    /**
     * 🔄 Chuyển đổi Category sang DTO
     */
    private CategoryMenuDTO convertToDTO(Category category, List<Category> allCategories) {
        CategoryMenuDTO dto = new CategoryMenuDTO();
        dto.setId(category.getId());
        dto.setTitle(category.getName());
        dto.setUrl("/alotra-website/products?category=" + category.getSlug());
        dto.setIcon("fas fa-mug-hot"); // Icon trà sữa

        // Tìm các category con
        List<Category> children = allCategories.stream()
                .filter(c -> c.getParent() != null && c.getParent().getId().equals(category.getId()))
                .collect(Collectors.toList());

        if (!children.isEmpty()) {
            List<CategoryMenuDTO> childDTOs = children.stream()
                    .map(child -> convertToDTO(child, allCategories))
                    .collect(Collectors.toList());
            dto.setChildren(childDTOs);
        } else {
            dto.setChildren(new ArrayList<>());
        }

        return dto;
    }

    /**
     * 📋 DTO cho Menu Item
     */
    public static class CategoryMenuDTO {
        private Long id;
        private String title;
        private String url;
        private String icon;
        private List<CategoryMenuDTO> children;

        // Getters & Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public List<CategoryMenuDTO> getChildren() { return children; }
        public void setChildren(List<CategoryMenuDTO> children) { this.children = children; }

        public boolean isHasChildren() {
            return children != null && !children.isEmpty();
        }
    }
}