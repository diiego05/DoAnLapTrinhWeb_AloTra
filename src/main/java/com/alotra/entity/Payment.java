package com.alotra.entity;

import com.alotra.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔸 Liên kết tới đơn hàng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false)
    private Order order;

    @Column(name = "Gateway", length = 50, nullable = false)
    private String gateway;  // VNPay, MoMo, PayOS, COD,...

    @Column(name = "PaymentMethod", length = 50, nullable = false)
    private String paymentMethod;

    @Column(name = "Amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "TransactionCode", length = 100)
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50, nullable = false)
    private PaymentStatus status;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "PaidAt")
    private LocalDateTime paidAt;

    @Lob
    @Column(name = "RawResponse")
    private String rawResponse;

    // 🆕 Trạng thái hoàn tiền
    @Column(name = "RefundStatus", length = 50)
    private String refundStatus;

    // 🆕 Số lần retry thanh toán
    @Column(name = "RetryCount", nullable = false)
    private int retryCount;
}
