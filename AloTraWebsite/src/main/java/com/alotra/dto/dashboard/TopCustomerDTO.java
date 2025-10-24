// 📁 src/main/java/com/alotra/dto/dashboard/TopCustomerDTO.java
package com.alotra.dto.dashboard;

import java.math.BigDecimal;

public class TopCustomerDTO {

    private String fullName;
    private long totalOrders;
    private BigDecimal totalSpent;

    public TopCustomerDTO(String fullName, long totalOrders, BigDecimal totalSpent) {
        this.fullName = fullName;
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }
}
