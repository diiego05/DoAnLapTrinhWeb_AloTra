package com.alotra.dto;

import java.math.BigDecimal;

public class ProductVariantDTO {

    private Long id;
    private Long sizeId;
    private String sizeName;
    private BigDecimal price;
    private String status;

    public ProductVariantDTO() {
    }

    // 🟢 Constructor cũ (giữ tương thích ngược với code cũ)
    public ProductVariantDTO(Long id, Long sizeId, BigDecimal price) {
        this.id = id;
        this.sizeId = sizeId;
        this.price = price;
    }

    // 🟢 Constructor mới (đầy đủ thông tin)
    public ProductVariantDTO(Long id, Long sizeId, String sizeName, BigDecimal price, String status) {
        this.id = id;
        this.sizeId = sizeId;
        this.sizeName = sizeName;
        this.price = price;
        this.status = status;
    }

    // === Getters & Setters ===
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSizeId() {
        return sizeId;
    }

    public void setSizeId(Long sizeId) {
        this.sizeId = sizeId;
    }

    public String getSizeName() {
        return sizeName;
    }

    public void setSizeName(String sizeName) {
        this.sizeName = sizeName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
