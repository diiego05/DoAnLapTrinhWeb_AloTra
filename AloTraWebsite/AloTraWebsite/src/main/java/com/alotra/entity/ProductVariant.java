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

    // --- Quan hệ tới Size ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SizeId", nullable = false)
    private Size size;

    @Column(name = "Price", nullable = false)
    private BigDecimal price;

    @Column(name = "Sku", length = 100)
    private String sku;

    // --- Thêm cột Status với giá trị mặc định ---
    @Column(name = "Status", nullable = false, length = 20)
    private String status = "ACTIVE";

    // --- Quan hệ tới Product ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    // --- Constructors ---
    public ProductVariant() {
        // đảm bảo luôn có giá trị mặc định khi tạo mới
        this.status = "ACTIVE";
    }

    // --- Getters & Setters ---
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
