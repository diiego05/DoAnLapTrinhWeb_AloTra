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

    /**
     * 📜 Danh sách tất cả đơn hàng
     * Có thể lọc theo branchId, status (trạng thái đơn hàng)
     * và sau này có thể mở rộng thêm lọc theo trạng thái thanh toán nếu cần
     */
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrders(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(orderService.getOrdersForAdmin(branchId, status));
    }

    /**
     * 📦 Chi tiết đơn hàng (bao gồm cả thông tin thanh toán mới nhất)
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailDTO> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetailForAdmin(orderId));
    }
}
