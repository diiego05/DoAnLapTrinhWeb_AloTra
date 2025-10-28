package com.alotra.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CouponValidationResult {
    private String type;          // "PERCENT" | "AMOUNT"
    private Integer percent;      // nếu type=PERCENT
    private BigDecimal amount;    // nếu type=AMOUNT
    private BigDecimal maxDiscount;
    private List<String> appliesTo; // ["ORDER"], ["SHIPPING"], hoặc ["ORDER","SHIPPING"]

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Computed {
        private BigDecimal items;     // giảm trên hàng
        private BigDecimal shipping;  // giảm trên ship (có thể null nếu chưa tính ở BE)
    }
    private Computed computed;
}
