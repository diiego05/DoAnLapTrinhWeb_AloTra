package com.alotra.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopCustomerStatisticDTO {
    private Long customerId;
    private String customerName;
    private Long totalOrders;
    private BigDecimal totalSpent;
}
