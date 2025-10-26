package com.alotra.dto.dashboard;

import java.math.BigDecimal;

public record TopProductDTO(
        String name,
        long quantity,
        BigDecimal revenue
) {}
