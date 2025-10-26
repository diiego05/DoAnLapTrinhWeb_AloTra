package com.alotra.controller.api;

import com.alotra.dto.OrderDTO;
import com.alotra.service.OrderService;
import com.alotra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/orders")
@RequiredArgsConstructor
public class OrderVendorApiController {

    private final OrderService orderService;
    private final UserService userService;

    /**
     * 🧾 Danh sách đơn hàng theo chi nhánh của vendor
     * ✅ OrderDTO đã bao gồm thông tin thanh toán mới nhất (PaymentDTO)
     */
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrders(@RequestParam(required = false) String status) {
        Long vendorId = userService.getCurrentUserId();
        List<OrderDTO> orders = orderService.getOrdersByVendor(vendorId, status);
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
