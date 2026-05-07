package com.alotra.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String imageUrl;
    private Long variantId;
    private Long sizeId;
    private String sizeName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private List<CartItemToppingDTO> toppings;
    private String note;
    private BigDecimal lineTotal;

    // 🆕 Thêm trường tổng tiền topping (nếu muốn lưu sẵn)
    private BigDecimal toppingTotal;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getVariantId() {
        return variantId;
    }
    public void setVariantId(Long variantId) {
        this.variantId = variantId;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public List<CartItemToppingDTO> getToppings() {
        return toppings;
    }
    public void setToppings(List<CartItemToppingDTO> toppings) {
        this.toppings = toppings;
    }

    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public BigDecimal getToppingTotal() {
        if (toppingTotal != null) return toppingTotal;
        if (toppings == null || toppings.isEmpty()) return BigDecimal.ZERO;
        return toppings.stream()
                .map(CartItemToppingDTO::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void setToppingTotal(BigDecimal toppingTotal) {
        this.toppingTotal = toppingTotal;
    }
}
