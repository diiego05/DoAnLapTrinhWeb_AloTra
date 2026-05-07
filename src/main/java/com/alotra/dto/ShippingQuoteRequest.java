package com.alotra.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShippingQuoteRequest {
    private Long carrierId;
    private BigDecimal afterMarkdown; // Tổng tiền hàng sau markdown
    private String couponCode;        // Mã freeship (nếu có)
}
