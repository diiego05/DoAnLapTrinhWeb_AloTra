package com.alotra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipperSummaryDTO {
    private long completedOrders;
    private long inProgressOrders;
    private BigDecimal totalEarnings;
}
