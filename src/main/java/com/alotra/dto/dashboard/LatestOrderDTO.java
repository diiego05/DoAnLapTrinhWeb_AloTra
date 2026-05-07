package com.alotra.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LatestOrderDTO {

    private String code;
    private String customerName;
    private BigDecimal total;
    private String status;
    private LocalDateTime createdAt;

    public LatestOrderDTO(String code, String customerName, BigDecimal total, String status, LocalDateTime createdAt) {
        this.code = code;
        this.customerName = customerName;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
