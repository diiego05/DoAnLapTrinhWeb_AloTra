package com.alotra.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingFeeDTO {
    private Long id;
    private String name;
    private BigDecimal baseFee;
    private BigDecimal discountedFee;
    private boolean hasDiscount;
    private int discountPercent;
}
