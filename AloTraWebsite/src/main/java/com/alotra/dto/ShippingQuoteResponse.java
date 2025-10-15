package com.alotra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ShippingQuoteResponse {
    private BigDecimal baseFee;
    private BigDecimal shippingDiscount;
    private BigDecimal finalShippingFee;
    private Long appliedCampaignId;
    private Long appliedCouponId;
}
