package com.alotra.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenuePointDTO(
        LocalDate date,
        BigDecimal revenue
) {}
