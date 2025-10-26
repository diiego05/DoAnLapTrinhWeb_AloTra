package com.alotra.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShipperSummaryDTO {
    private long todayOrders;
    private long inProgressOrders;
    private long completedOrders;
    private String totalEarnings;
}
