package com.alotra.dto.dashboard;

import java.math.BigDecimal;
import java.sql.Date;

public class OrderByDateDTO {

    private Date date;
    private Long orderCount;
    private BigDecimal totalEarnings;

    public OrderByDateDTO(Date date, Long orderCount, Double totalEarnings) {
        this.date = date;
        this.orderCount = orderCount;
        this.totalEarnings = BigDecimal.valueOf(totalEarnings != null ? totalEarnings : 0);
    }

    public Date getDate() {
        return date;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }
}
