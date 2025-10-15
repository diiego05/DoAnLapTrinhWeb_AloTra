package com.alotra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class LineItemPrice {
    private Long productId;
    private BigDecimal originalPrice;     // Tổng giá gốc (unitPrice * qty)
    private BigDecimal finalPrice;        // Tổng giá sau giảm
    private Integer quantity;
    private Long appliedCampaignId;       // Campaign áp dụng nếu có
    private BigDecimal discountAmount;    // Tổng tiền đã giảm trên dòng sản phẩm
}
