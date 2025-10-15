package com.alotra.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponDTO(
        Long id,
        String code,
        String type,
        BigDecimal value,
        BigDecimal maxDiscount,
        BigDecimal minOrderTotal,
        Integer usageLimit,
        Integer usedCount,
        String campaignName,
        LocalDateTime startAt,
        LocalDateTime endAt
) {}
