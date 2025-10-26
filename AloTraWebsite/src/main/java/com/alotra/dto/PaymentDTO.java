package com.alotra.dto;

import com.alotra.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentDTO {

    private Long id;                     // ID của bản ghi thanh toán
    private String gateway;              // Cổng thanh toán (COD, VNPAY, MOMO, v.v.)
    private String paymentMethod;        // Hình thức thanh toán (COD, BANK, PICKUP,...)
    private BigDecimal amount;           // Số tiền thanh toán
    private PaymentStatus status;        // ✅ Trạng thái thanh toán (PENDING, SUCCESS,...)
    private LocalDateTime createdAt;     // Thời điểm tạo thanh toán
    private LocalDateTime paidAt;        // Thời điểm thanh toán thành công (nếu có)
    private String refundStatus;         // Trạng thái hoàn tiền (nếu có)
    private String transactionCode;      // ✅ Mã giao dịch (liên kết với Order)
}
