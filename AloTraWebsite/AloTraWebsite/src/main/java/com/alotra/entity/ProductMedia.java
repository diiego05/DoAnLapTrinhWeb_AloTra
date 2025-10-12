package com.alotra.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ProductMedia")
public class ProductMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "Url", nullable = false)
    private String url;

    @Column(name = "IsPrimary")
    private boolean isPrimary;

    // Mối quan hệ: Nhiều media thuộc về một sản phẩm
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean isPrimary) { this.isPrimary = isPrimary; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}