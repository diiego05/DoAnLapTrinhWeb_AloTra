package com.alotra.controller.api;

import com.alotra.dto.OrderDTO;
import com.alotra.dto.OrderDetailDTO;
import com.alotra.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminApiController {

    private final OrderService orderService;

    // 📜 Danh sách tất cả đơn hàng (lọc theo branch + status nếu có)
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrders(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getOrdersForAdmin(branchId, status));
    }

    // 📦 Chi tiết đơn hàng
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailDTO> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetailForAdmin(orderId));
    }
}
