package com.alotra.entity;

import java.math.BigDecimal;

public class CartItem {

    private ProductVariant variant;
    private int quantity;

    public CartItem(ProductVariant variant, int quantity) {
        this.variant = variant;
        this.quantity = quantity;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return variant.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
