package com.alotra.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RecentOrderDTO {
    private Long orderId;
    private String code;
    private String customerName;
    private String address;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;

    public RecentOrderDTO(Long orderId, String code, String customerName, String address,
                          Double totalAmount, String status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.code = code;
        this.customerName = customerName;
        this.address = address;
        this.totalAmount = BigDecimal.valueOf(totalAmount != null ? totalAmount : 0);
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getOrderId() { return orderId; }
    public String getCode() { return code; }
    public String getCustomerName() { return customerName; }
    public String getAddress() { return address; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
