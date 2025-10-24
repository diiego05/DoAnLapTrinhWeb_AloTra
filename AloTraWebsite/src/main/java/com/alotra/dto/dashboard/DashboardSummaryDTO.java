package com.alotra.dto.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryDTO(
        long totalUsers,
        long totalOrders,
        long totalBranches,
        long totalShippers,
        BigDecimal totalRevenue,
        long activePromotions
) {}
