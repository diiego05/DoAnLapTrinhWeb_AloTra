package com.alotra.controller.api;

import com.alotra.dto.OrderDTO;
import com.alotra.dto.OrderStatusHistoryDTO;
import com.alotra.dto.PaymentDTO;
import com.alotra.entity.Order;
import com.alotra.entity.OrderStatusHistory;
import com.alotra.entity.Payment;
import com.alotra.entity.ShippingAssignment;
import com.alotra.enums.OrderStatus;
import com.alotra.enums.PaymentMethod;
import com.alotra.enums.PaymentStatus;
import com.alotra.repository.OrderRepository;
import com.alotra.repository.OrderStatusHistoryRepository;
import com.alotra.repository.PaymentRepository;
import com.alotra.repository.ShippingAssignmentRepository;
import com.alotra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shipper/orders")
@RequiredArgsConstructor
public class ShipperOrderApiController {

    private final ShippingAssignmentRepository shippingAssignmentRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;

    // ======================= 📦 Lấy tất cả đơn hàng được phân công cho shipper =======================
    @GetMapping
    public List<OrderDTO> getAssignedOrders() {
        Long shipperId = userService.getCurrentShipperId();

        List<Long> orderIds = shippingAssignmentRepository
                .findByShipperId(shipperId)
                .stream()
                .map(ShippingAssignment::getOrderId)
                .toList();

        List<Order> orders = orderRepository.findAllById(orderIds);
        orders.sort(Comparator.comparing(Order::getCreatedAt).reversed());

        return orders.stream()
                .map(this::mapToOrderDTOWithPayment)
                .collect(Collectors.toList());
    }

    // ======================= 📄 Lấy chi tiết đơn hàng =======================
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderDetail(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        return ResponseEntity.ok(mapToOrderDTOWithPayment(order));
    }

    // ======================= ✅ Shipper nhận đơn =======================
    @Transactional
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptOrder(@PathVariable Long id) {
        Long shipperId = userService.getCurrentShipperId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!OrderStatus.WAITING_FOR_PICKUP.name().equals(order.getStatus())) {
            return ResponseEntity.badRequest().body("Đơn hàng không ở trạng thái chờ nhận");
        }

        ShippingAssignment assignment = shippingAssignmentRepository
                .findByOrderIdAndShipperId(id, shipperId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân công"));

        assignment.setStatus("ACCEPTED");
        assignment.setAssignedAt(LocalDateTime.now());
        shippingAssignmentRepository.save(assignment);

        shippingAssignmentRepository.findByOrderId(id).forEach(a -> {
            if (!a.getShipperId().equals(shipperId)) {
                a.setStatus("LOCKED");
                shippingAssignmentRepository.save(a);
            }
        });

        order.setStatus(OrderStatus.SHIPPING.name());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

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

    // ======================= 🚀 Shipper giao hàng thành công =======================
    @Transactional
    @PutMapping("/{id}/delivered")
    public ResponseEntity<?> markDelivered(@PathVariable Long id) {
        Long shipperId = userService.getCurrentShipperId();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!OrderStatus.SHIPPING.name().equals(order.getStatus())) {
            return ResponseEntity.badRequest().body("Đơn hàng không ở trạng thái đang giao");
        }

        ShippingAssignment assignment = shippingAssignmentRepository
                .findByOrderIdAndShipperId(id, shipperId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân công"));

        assignment.setStatus("DELIVERED");
        assignment.setDeliveredAt(LocalDateTime.now());
        shippingAssignmentRepository.save(assignment);

        order.setStatus(OrderStatus.COMPLETED.name());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        orderStatusHistoryRepository.save(
                OrderStatusHistory.builder()
                        .order(order)
                        .status(OrderStatus.COMPLETED.name())
                        .note("Shipper đã giao thành công")
                        .changedAt(LocalDateTime.now())
                        .build()
        );

        // ✅ COD: giao thành công => cập nhật thanh toán SUCCESS
        if (order.getPaymentMethod() != null
                && order.getPaymentMethod().equalsIgnoreCase(PaymentMethod.COD.name())) {

            Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getId()).orElse(null);
            if (payment != null) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
        }

        return ResponseEntity.ok().build();
    }

    // ======================= 🧭 Mapper có payment =======================
    private OrderDTO mapToOrderDTOWithPayment(Order order) {
        var history = orderStatusHistoryRepository
                .findByOrderIdOrderByChangedAtAsc(order.getId())
                .stream()
                .map(h -> OrderStatusHistoryDTO.builder()
                        .status(h.getStatus())
                        .note(h.getNote())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();

        // ✅ Lấy payment mới nhất theo createdAt (tránh paidAt null làm sai)
        var payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getId()).orElse(null);

        PaymentDTO paymentDTO = null;
        if (payment != null) {
            paymentDTO = PaymentDTO.builder()
                    .id(payment.getId())
                    .gateway(payment.getGateway())
                    .paymentMethod(payment.getPaymentMethod())
                    .amount(payment.getAmount())
                    .status(payment.getStatus())
                    .createdAt(payment.getCreatedAt())
                    .paidAt(payment.getPaidAt())
                    .refundStatus(payment.getRefundStatus())
                    .build();
        }

        return OrderDTO.builder()
                .id(order.getId())
                .code(order.getCode())
                .status(order.getStatus())
                .total(order.getTotal())
                .deliveryAddress(order.getDeliveryAddress())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .statusHistory(history)
                .payment(paymentDTO)
                .build();
    }
}
