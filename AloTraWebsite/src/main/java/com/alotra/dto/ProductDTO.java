// 📁 com/alotra/dto/ProductDTO.java
package com.alotra.dto;

import java.math.BigDecimal;

public class ProductDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private BigDecimal price;

    // Constructor, Getters, and Setters
    public ProductDTO(Long id, String name, String imageUrl, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.price = price;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}