package com.alotra.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private String code;
    private BigDecimal total;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
    private String paymentMethod;
    private String branchName;
    private String deliveryAddress;
    private List<OrderStatusHistoryDTO> statusHistory;

    // 🆕 Thêm thông tin thanh toán (nếu có)
    private PaymentDTO payment;
}
