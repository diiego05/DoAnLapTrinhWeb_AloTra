package com.alotra.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "ProductVariants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SizeId", nullable = false)
    private Size size;

    // 💰 Đây là giá gốc lưu trong DB
    @Column(name = "Price", nullable = false)
    private BigDecimal price;

    @Column(name = "Sku", length = 100)
    private String sku;

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    // 🪄 Trường không lưu vào DB — chỉ để truyền ra view
    @Transient
    private BigDecimal discountedPrice;

    @Transient
    private Integer discountPercent;

    // === Constructors ===
    public ProductVariant() {
        this.status = "ACTIVE";
    }

    // === Getters & Setters ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Size getSize() { return size; }
    public void setSize(Size size) { this.size = size; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getDiscountedPrice() { return discountedPrice; }
    public void setDiscountedPrice(BigDecimal discountedPrice) { this.discountedPrice = discountedPrice; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    // ✅ Để Thymeleaf có thể dùng variant.originalPrice mà không thêm cột
    @Transient
    public BigDecimal getOriginalPrice() {
        return this.price;
    }

    @Transient
    public boolean getHasDiscount() {
        return discountedPrice != null
            && price != null
            && discountedPrice.compareTo(price) < 0;
    }
}
