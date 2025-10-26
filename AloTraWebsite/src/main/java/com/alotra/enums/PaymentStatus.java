package com.alotra.enums;

public enum PaymentStatus {
    PENDING,          // ⏳ Đang chờ thanh toán
    SUCCESS,          // ✅ Thanh toán thành công
    FAILED,           // ❌ Thanh toán thất bại
    REFUNDED,         // 💸 Đã hoàn tiền
    CANCELED          // ❌ Đã hủy giao dịch
}
