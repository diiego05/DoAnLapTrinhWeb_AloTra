package com.alotra.controller.api;

import com.alotra.dto.OrderDTO;
import com.alotra.service.OrderService;
import com.alotra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/vendor/orders")
@RequiredArgsConstructor
public class OrderVendorApiController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * 🧾 Danh sách đơn hàng theo chi nhánh của vendor
     * Hỗ trợ lọc theo trạng thái, thời gian và từ khóa tìm kiếm mã đơn.
     */
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "q", required = false) String keyword
    ) {
        Long vendorId = userService.getCurrentUserId();
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(LocalTime.MAX) : null;
        List<OrderDTO> orders = orderService.getOrdersByVendor(vendorId, status, fromDt, toDt, keyword);
        return ResponseEntity.ok(orders);
    }

    /**
     * ✅ Duyệt đơn
     */
    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirmOrder(@PathVariable Long orderId) {
        Long vendorId = userService.getCurrentUserId();
        orderService.confirmOrderByVendor(orderId, vendorId);
        return ResponseEntity.ok().build();
    }

    /**
     * 🚚 Giao shipper
     */
    @PutMapping("/{orderId}/ship")
    public ResponseEntity<Void> shipOrder(@PathVariable Long orderId) {
        Long vendorId = userService.getCurrentUserId();
        orderService.shipOrderByVendor(orderId, vendorId);
        return ResponseEntity.ok().build();
    }

    /**
     * ❌ Hủy đơn (chỉ PENDING)
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        Long vendorId = userService.getCurrentUserId();
        orderService.cancelOrderByVendor(orderId, vendorId);
        return ResponseEntity.ok().build();
    }
}