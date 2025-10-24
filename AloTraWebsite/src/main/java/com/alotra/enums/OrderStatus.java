// com/alotra/enums/OrderStatus.java
package com.alotra.enums;

public enum OrderStatus {
    PENDING,              // Chờ xác nhận
    CONFIRMED,            // Đã xác nhận (trước khi giao cho shipper)
    WAITING_FOR_PICKUP,   // ✅ Chờ shipper nhận đơn
    SHIPPING,             // 🚚 Đang giao hàng
    DELIVERED,            // 🟢 Đã giao (tùy chọn nếu cần tách khỏi COMPLETED)
    COMPLETED,            // ✅ Hoàn thành đơn
    CANCELED,             // ❌ Đã hủy
    PAID,                 // 💰 Đã thanh toán
    FAILED,               // ❌ Thanh toán thất bại
    AWAITING_PAYMENT      // ⏳ Chờ thanh toán
}
