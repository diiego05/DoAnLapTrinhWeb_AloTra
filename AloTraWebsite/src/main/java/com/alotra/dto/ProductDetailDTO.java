package com.alotra.dto;

import java.util.List;

public class ProductDetailDTO {

    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName; // 🟢 Thêm tên danh mục
    private String status;
    private List<ProductVariantDTO> variants;
    private List<String> imageUrls;

    public ProductDetailDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ProductVariantDTO> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariantDTO> variants) {
        this.variants = variants;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
