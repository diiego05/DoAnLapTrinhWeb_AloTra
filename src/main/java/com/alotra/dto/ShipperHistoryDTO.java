package com.alotra.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ShipperHistoryDTO {
    private String orderCode;
    private LocalDateTime deliveredAt;
    private BigDecimal shippingFee;
    private BigDecimal earnings;

    // ✅ Phải có constructor này
    public ShipperHistoryDTO(String orderCode, LocalDateTime deliveredAt, BigDecimal shippingFee, Double earnings) {
        this.orderCode = orderCode;
        this.deliveredAt = deliveredAt;
        this.shippingFee = shippingFee;
        this.earnings = BigDecimal.valueOf(earnings);
    }

}
