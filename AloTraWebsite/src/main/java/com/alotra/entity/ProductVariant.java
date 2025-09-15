// 📁 com/alotra/entity/ProductVariant.java
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

    // --- CÁC THAY ĐỔI NẰM Ở ĐÂY ---

    // 1. Thay thế "String size" bằng mối quan hệ với Entity Size
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SizeId")
    private Size size;

    @Column(name = "Price", nullable = false)
    private BigDecimal price;

    // 2. Thêm thuộc tính Sku
    @Column(name = "Sku")
    private String sku;

    // 3. Thêm thuộc tính Status
    @Column(name = "Status")
    private String status;

    // --- HẾT PHẦN THAY ĐỔI ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    // Getters and Setters (bao gồm cả các thuộc tính mới)
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
}