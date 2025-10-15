package com.alotra.dto;

import java.math.BigDecimal;

public class ProductVariantDTO {

    private Long id;
    private Long sizeId;

    // 🆕 Thêm sizeCode để dùng cho UI hiển thị ("S", "M", "L"...)
    private String sizeCode;

    // 📝 Giữ lại sizeName nếu có nơi khác dùng (vd: “Size M”)
    private String sizeName;

    private BigDecimal price;
    private String status;

    // 🏷️ Thông tin giảm giá
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private boolean hasDiscount;
    private int discountPercent;

    public ProductVariantDTO() {
    }

    // 🟢 Constructor cũ (giữ tương thích ngược)
    public ProductVariantDTO(Long id, Long sizeId, BigDecimal price) {
        this.id = id;
        this.sizeId = sizeId;
        this.price = price;
    }

    // 🟢 Constructor có tên size
    public ProductVariantDTO(Long id, Long sizeId, String sizeName, BigDecimal price, String status) {
        this.id = id;
        this.sizeId = sizeId;
        this.sizeName = sizeName;
        this.price = price;
        this.status = status;
    }

    // 🆕 Constructor mới có sizeCode
    public ProductVariantDTO(Long id, Long sizeId, String sizeCode, String sizeName, BigDecimal price, String status) {
        this.id = id;
        this.sizeId = sizeId;
        this.sizeCode = sizeCode;
        this.sizeName = sizeName;
        this.price = price;
        this.status = status;
    }

    // 🆕 Constructor đầy đủ có giảm giá + sizeCode
    public ProductVariantDTO(Long id,
                             Long sizeId,
                             String sizeCode,
                             String sizeName,
                             BigDecimal originalPrice,
                             BigDecimal discountedPrice,
                             boolean hasDiscount,
                             int discountPercent,
                             String status) {
        this.id = id;
        this.sizeId = sizeId;
        this.sizeCode = sizeCode;
        this.sizeName = sizeName;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.hasDiscount = hasDiscount;
        this.discountPercent = discountPercent;
        this.status = status;
        // ⚠️ Giữ price bằng giá sau giảm để tương thích code cũ
        this.price = discountedPrice;
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

    public String getSizeCode() {
        return sizeCode;
    }

    public void setSizeCode(String sizeCode) {
        this.sizeCode = sizeCode;
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

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public boolean isHasDiscount() {
        return hasDiscount;
    }

    public void setHasDiscount(boolean hasDiscount) {
        this.hasDiscount = hasDiscount;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }
}
