package com.alotra.controller.api;

import com.alotra.dto.OrderDTO;
import com.alotra.dto.OrderStatusHistoryDTO;
import com.alotra.entity.Order;
import com.alotra.entity.OrderStatusHistory;
import com.alotra.entity.ShippingAssignment;
import com.alotra.enums.OrderStatus;
import com.alotra.repository.OrderRepository;
import com.alotra.repository.OrderStatusHistoryRepository;
import com.alotra.repository.ShippingAssignmentRepository;
import com.alotra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/shipper/orders")
@RequiredArgsConstructor
public class ShipperOrderApiController {

    private final ShippingAssignmentRepository shippingAssignmentRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserService userService;

    // ======================= 📦 Lấy danh sách đơn được phân công =======================
    @GetMapping
    public List<OrderDTO> getAssignedOrders() {
        Long shipperId = userService.getCurrentShipperId();

        List<Long> orderIds = shippingAssignmentRepository
                .findByShipperIdAndStatusIn(shipperId, List.of("PENDING", "ACCEPTED"))
                .stream()
                .map(ShippingAssignment::getOrderId)
                .toList();

        return orderRepository.findAllById(orderIds).stream()
                .map(this::mapToOrderDTO)
                .toList();
    }

    // ======================= 📄 Lấy chi tiết đơn hàng =======================
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderDetail(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        return ResponseEntity.ok(mapToOrderDTO(order));
    }

    // ======================= ✅ Shipper nhận đơn =======================
    @Transactional
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptOrder(@PathVariable Long id) {
        Long shipperId = userService.getCurrentShipperId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!OrderStatus.CONFIRMED.name().equals(order.getStatus())) {
            return ResponseEntity.badRequest().body("Đơn hàng không thể nhận");
        }

        // ✅ Shipper hiện tại nhận đơn
        ShippingAssignment assignment = shippingAssignmentRepository
                .findByOrderIdAndShipperId(id, shipperId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân công"));

        assignment.setStatus("ACCEPTED");
        assignment.setAssignedAt(LocalDateTime.now());
        shippingAssignmentRepository.save(assignment);

        // ❌ Các shipper khác bị khóa
        shippingAssignmentRepository.findByOrderId(id).forEach(a -> {
            if (!a.getShipperId().equals(shipperId)) {
                a.setStatus("LOCKED");
                shippingAssignmentRepository.save(a);
            }
        });

        // 🚚 Cập nhật trạng thái đơn hàng
        order.setStatus(OrderStatus.SHIPPING.name());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 📝 Ghi lịch sử trạng thái
        orderStatusHistoryRepository.save(
                OrderStatusHistory.builder()
                        .order(order)
                        .status(OrderStatus.SHIPPING.name())
                        .note("Shipper đã nhận đơn")
                        .changedAt(LocalDateTime.now())
                        .build()
        );

        return ResponseEntity.ok().build();
    }

    // ======================= 🚀 Giao hàng thành công =======================
    @Transactional
    @PutMapping("/{id}/delivered")
    public ResponseEntity<?> markDelivered(@PathVariable Long id) {
        Long shipperId = userService.getCurrentShipperId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!OrderStatus.SHIPPING.name().equals(order.getStatus())) {
            return ResponseEntity.badRequest().body("Đơn hàng không ở trạng thái giao hàng");
        }

        // ✅ Xác nhận assignment của shipper
        ShippingAssignment assignment = shippingAssignmentRepository
                .findByOrderIdAndShipperId(id, shipperId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân công"));

        assignment.setStatus("DELIVERED");
        assignment.setDeliveredAt(LocalDateTime.now());
        shippingAssignmentRepository.save(assignment);

        // 🟢 Cập nhật trạng thái đơn
        order.setStatus(OrderStatus.COMPLETED.name());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 📝 Ghi lịch sử trạng thái (đúng trạng thái COMPLETED)
        orderStatusHistoryRepository.save(
                OrderStatusHistory.builder()
                        .order(order)
                        .status(OrderStatus.COMPLETED.name())
                        .note("Shipper đã giao thành công")
                        .changedAt(LocalDateTime.now())
                        .build()
        );

        return ResponseEntity.ok().build();
    }

    // ======================= 🧭 Mapper =======================
    private OrderDTO mapToOrderDTO(Order order) {
        var history = orderStatusHistoryRepository
                .findByOrderIdOrderByChangedAtAsc(order.getId())
                .stream()
                .map(h -> OrderStatusHistoryDTO.builder()
                        .status(h.getStatus())
                        .note(h.getNote())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();

        return OrderDTO.builder()
                .id(order.getId())
                .code(order.getCode())
                .status(order.getStatus())
                .total(order.getTotal())
                .deliveryAddress(order.getDeliveryAddress())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .statusHistory(history)
                .build();
    }
}
