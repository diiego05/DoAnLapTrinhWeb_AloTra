package com.alotra.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDetailDTO {
    private Long id;
    private String code;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discount;
    private BigDecimal total;
    private String paymentMethod;
    private String deliveryAddress;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
    private List<OrderStatusHistoryDTO> statusHistory;
    private String branchName;

    // 🆕 Thêm thông tin thanh toán mới nhất
    private PaymentDTO payment;
}
