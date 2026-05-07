package com.alotra.dto.dashboard;

public record OrderStatusCountDTO(
        String status,
        long count
) {}
