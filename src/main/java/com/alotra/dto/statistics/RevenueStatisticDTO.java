package com.alotra.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticDTO {
    private LocalDate date;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal avgPerOrder;
}
