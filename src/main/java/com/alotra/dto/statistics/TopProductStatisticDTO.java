package com.alotra.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductStatisticDTO {
    private Long productId;
    private String productName;
    private Long totalQuantity;
    private BigDecimal totalRevenue;
}
