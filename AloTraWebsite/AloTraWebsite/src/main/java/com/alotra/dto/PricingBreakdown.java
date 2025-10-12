package com.alotra.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PricingBreakdown {
    private List<LineItemPrice> items;

    private BigDecimal itemsSubtotal;       // Tổng giá gốc của sản phẩm
    private BigDecimal markdownDiscount;    // Giảm giá từ campaign markdown
    private BigDecimal afterMarkdown;       // Tổng tiền sau markdown

    private BigDecimal couponDiscount;      // Giảm từ coupon ORDER_*
    private String appliedCouponCode;
    private Long appliedCouponId;

    private BigDecimal shippingFee;         // Phí ship gốc
    private BigDecimal shippingDiscount;    // Giảm phí ship (freeship)
    private Long appliedShippingCampaignId;
    private Long appliedShippingCouponId;

    private BigDecimal grandTotal;          // Tổng tiền cuối cùng
}
