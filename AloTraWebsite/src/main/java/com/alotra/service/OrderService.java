/*package com.alotra.service;

import com.alotra.dto.checkout.CheckoutRequestDTO;
import com.alotra.dto.checkout.OrderResponseDTO;
import com.alotra.entity.Order;
import com.alotra.entity.OrderItem;
import com.alotra.entity.OrderItemTopping;
import com.alotra.entity.OrderStatusHistory;
import com.alotra.enums.OrderStatus;
import com.alotra.enums.PaymentMethod;
import com.alotra.repository.OrderItemRepository;
import com.alotra.repository.OrderItemToppingRepository;
import com.alotra.repository.OrderRepository;
import com.alotra.repository.OrderStatusHistoryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final CouponService couponService;
    private final ShippingCarrierService shippingCarrierService;
    private final AddressService addressService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemToppingRepository orderItemToppingRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final BranchService branchService;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderStatusHistory orderStatusHistory;


    @Transactional
    public OrderResponseDTO checkout(Long userId, CheckoutRequestDTO req) {
        // 1️⃣ Kiểm tra cart item
        if (req.getCartItemIds() == null || req.getCartItemIds().isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm nào để thanh toán");
        }

        // 2️⃣ Lấy snapshot cart item
        var items = cartService.getItemDetailsByIds(userId, req.getCartItemIds());
        if (items.isEmpty()) throw new IllegalArgumentException("Cart items không hợp lệ");

        // ✅ 2.1️⃣ Kiểm tra khả dụng của các item theo chi nhánh đã chọn
        List<Long> unavailableIds = branchService.checkCartItemAvailability(req.getBranchId(), req.getCartItemIds());
        if (!unavailableIds.isEmpty()) {
            throw new IllegalStateException("Một số sản phẩm không khả dụng tại chi nhánh này. Vui lòng chọn chi nhánh khác hoặc cập nhật giỏ hàng.");
        }

        // 3️⃣ Tính subtotal
        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().add(i.getToppingTotalEach())
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4️⃣ Tính shipping
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (req.getPaymentMethod() == null ||
                !req.getPaymentMethod().equalsIgnoreCase(PaymentMethod.PICKUP.name())) {
            var carrier = shippingCarrierService.findActiveById(req.getShippingCarrierId());
            shippingFee = carrier.getBaseFee();
        }

        // 5️⃣ Áp dụng coupon (nếu có)
        BigDecimal discount = BigDecimal.ZERO;
        Long couponId = null;
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            List<Long> productIds = items.stream()
                    .map(i -> i.getProductId())
                    .collect(Collectors.toList());

            var coupon = couponService.validateCoupon(req.getCouponCode(), subtotal, productIds);
            discount = couponService.calculateDiscount(coupon, subtotal);
            couponId = coupon.getId();
        }

        BigDecimal total = subtotal.subtract(discount).add(shippingFee);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        // 6️⃣ Snapshot địa chỉ
        String deliveryAddress = addressService.snapshotAddress(
                req.getAddressId(),
                userId,
                req.getPaymentMethod()
        );

        // 7️⃣ Tạo mã đơn hàng
        String code = generateOrderCode();

        // 8️⃣ Lưu Order
        Order order = Order.builder()
                .code(code)
                .userId(userId)
                .branchId(req.getBranchId())
                .shippingCarrierId(req.getShippingCarrierId())
                .couponId(couponId)
                .deliveryAddress(deliveryAddress)
                .paymentMethod(normalizePaymentMethod(req.getPaymentMethod()))
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discount(discount)
                .total(total)
                .status(OrderStatus.PENDING.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);
        final Order savedOrder = order;

        // 9️⃣ Lưu OrderItem
        List<OrderItem> orderItems = items.stream().map(ci -> OrderItem.builder()
                .order(savedOrder)
                .productId(ci.getProductId())
                .variantId(ci.getVariantId())
                .productName(ci.getProductName())
                .sizeName(ci.getSizeName())
                .note(ci.getNote())
                .quantity(ci.getQuantity())
                .unitPrice(ci.getUnitPrice())
                .toppingTotal(ci.getToppingTotalEach())
                .lineTotal(ci.getUnitPrice().add(ci.getToppingTotalEach())
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .build()
        ).collect(Collectors.toList());
        orderItemRepository.saveAll(orderItems);

        // 🆕 9️⃣.1 Lưu topping
        for (int i = 0; i < items.size(); i++) {
            var cartDetail = items.get(i);
            var orderItem = orderItems.get(i);

            if (cartDetail.getToppings() != null && !cartDetail.getToppings().isEmpty()) {
                var orderToppings = cartDetail.getToppings().stream()
                        .map(t -> OrderItemTopping.builder()
                                .orderItem(orderItem)
                                .toppingId(t.getToppingId())
                                .toppingName(t.getName())
                                .priceAtAddition(t.getPrice())
                                .build())
                        .toList();

                orderItemToppingRepository.saveAll(orderToppings);
            }
        }

        // 🔟 Clear giỏ hàng
        cartService.removeItems(userId, req.getCartItemIds());

        // 🪙 1️⃣0️⃣.1 Cập nhật số lần sử dụng coupon
        if (couponId != null) {
            couponService.increaseUsedCount(couponId);
        }

        // 🕓 1️⃣0️⃣.2 Lưu lịch sử trạng thái đơn hàng
        orderStatusHistoryRepository.save(
            OrderStatusHistory.builder()
                    .order(savedOrder)
                    .status(OrderStatus.PENDING.name())
                    .changedAt(LocalDateTime.now())
                    .note("Đơn hàng được khởi tạo")
                    .build()
        );

        // 1️⃣1️⃣ Gửi mail + thông báo
        safe(() -> emailService.sendOrderConfirmationEmail(userId, savedOrder, orderItems));
        safe(() -> notificationService.create(
                userId,
                "ORDER",
                "Đặt hàng thành công",
                "Đơn hàng #" + code + " đã được tạo thành công.",
                "Order",
                savedOrder.getId()
        ));

        // 1️⃣2️⃣ Trả về DTO
        return OrderResponseDTO.builder()
                .orderId(savedOrder.getId())
                .code(savedOrder.getCode())
                .status(savedOrder.getStatus())
                .subtotal(savedOrder.getSubtotal())
                .discount(savedOrder.getDiscount())
                .shippingFee(savedOrder.getShippingFee())
                .total(savedOrder.getTotal())
                .items(orderItems.stream().map(oi -> OrderResponseDTO.OrderedLineDTO.builder()
                        .productName(oi.getProductName())
                        .sizeName(oi.getSizeName())
                        .quantity(oi.getQuantity())
                        .unitPrice(oi.getUnitPrice())
                        .toppingTotal(oi.getToppingTotal())
                        .lineTotal(oi.getLineTotal())
                        .note(oi.getNote())
                        .build()).toList())
                .build();
    }


    private String normalizePaymentMethod(String pm) {
        if (pm == null) return PaymentMethod.COD.name();
        try {
            return PaymentMethod.valueOf(pm.toUpperCase()).name();
        } catch (Exception e) {
            return PaymentMethod.COD.name();
        }
    }

    private String generateOrderCode() {
        // ví dụ: ALO-20251014-xxxxx
        String ymd = java.time.LocalDate.now().toString().replaceAll("-", "");
        String rand = String.valueOf((int) (Math.random() * 90000) + 10000);
        return "ALO-" + ymd + "-" + rand;
    }

    private void safe(Runnable r) {
        try { r.run(); } catch (Exception ignored) {}
    }
}*/
package com.alotra.service;

import com.alotra.dto.OrderDTO;
import com.alotra.dto.OrderDetailDTO;
import com.alotra.dto.OrderItemDTO;
import com.alotra.dto.OrderStatusHistoryDTO;
import com.alotra.dto.checkout.CheckoutRequestDTO;
import com.alotra.dto.checkout.OrderResponseDTO;
import com.alotra.entity.*;
import com.alotra.enums.OrderStatus;
import com.alotra.enums.PaymentMethod;
import com.alotra.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alotra.dto.ToppingDTO;
import java.math.BigDecimal;
import com.alotra.repository.BranchRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final CouponService couponService;
    private final ShippingCarrierService shippingCarrierService;
    private final AddressService addressService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemToppingRepository orderItemToppingRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final BranchService branchService;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final BranchRepository branchRepository;

    @Autowired
    private ShipperRepository shipperRepository;

    @Autowired
    private ShippingAssignmentRepository shippingAssignmentRepository;
    // ====================================
    // 🧾 1. CHECKOUT
    // ====================================
    @Transactional
    public OrderResponseDTO checkout(Long userId, CheckoutRequestDTO req) {
        if (req.getCartItemIds() == null || req.getCartItemIds().isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm nào để thanh toán");
        }

        var items = cartService.getItemDetailsByIds(userId, req.getCartItemIds());
        if (items.isEmpty()) throw new IllegalArgumentException("Cart items không hợp lệ");

        var unavailable = branchService.checkCartItemAvailability(req.getBranchId(), req.getCartItemIds());
        if (!unavailable.isEmpty()) {
            throw new IllegalStateException("Một số sản phẩm không khả dụng tại chi nhánh này.");
        }

        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().add(i.getToppingTotalEach())
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = BigDecimal.ZERO;
        if (req.getPaymentMethod() == null ||
                !req.getPaymentMethod().equalsIgnoreCase(PaymentMethod.PICKUP.name())) {
            var carrier = shippingCarrierService.findActiveById(req.getShippingCarrierId());
            shippingFee = carrier.getBaseFee();
        }

        BigDecimal discount = BigDecimal.ZERO;
        Long couponId = null;
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            var productIds = items.stream().map(i -> i.getProductId()).toList();
            var coupon = couponService.validateCoupon(req.getCouponCode(), subtotal, productIds);
            discount = couponService.calculateDiscount(coupon, subtotal);
            couponId = coupon.getId();
        }

        BigDecimal total = subtotal.subtract(discount).add(shippingFee);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        String deliveryAddress = addressService.snapshotAddress(req.getAddressId(), userId, req.getPaymentMethod());

        Order order = Order.builder()
                .code(generateOrderCode())
                .userId(userId)
                .branchId(req.getBranchId())
                .shippingCarrierId(req.getShippingCarrierId())
                .couponId(couponId)
                .deliveryAddress(deliveryAddress)
                .paymentMethod(normalizePaymentMethod(req.getPaymentMethod()))
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discount(discount)
                .total(total)
                .status(OrderStatus.PENDING.name())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderRepository.save(order);

        List<OrderItem> orderItems = items.stream()
                .map(ci -> OrderItem.builder()
                        .order(order)
                        .productId(ci.getProductId())
                        .variantId(ci.getVariantId())
                        .productName(ci.getProductName())
                        .sizeName(ci.getSizeName())
                        .note(ci.getNote())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getUnitPrice())
                        .toppingTotal(ci.getToppingTotalEach())
                        .lineTotal(ci.getUnitPrice().add(ci.getToppingTotalEach())
                                .multiply(BigDecimal.valueOf(ci.getQuantity())))
                        .build())
                .toList();
        orderItemRepository.saveAll(orderItems);

        var toppingEntities = items.stream()
                .flatMap(cartItem -> {
                    int idx = items.indexOf(cartItem);
                    var orderItem = orderItems.get(idx);
                    return cartItem.getToppings() != null
                            ? cartItem.getToppings().stream().map(t -> OrderItemTopping.builder()
                            .orderItem(orderItem)
                            .toppingId(t.getToppingId())
                            .toppingName(t.getName())
                            .priceAtAddition(t.getPrice())
                            .build())
                            : null;
                })
                .filter(t -> t != null)
                .toList();
        if (!toppingEntities.isEmpty()) orderItemToppingRepository.saveAll(toppingEntities);

        cartService.removeItems(userId, req.getCartItemIds());

        if (couponId != null) couponService.increaseUsedCount(couponId);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PENDING.name())
                .changedAt(LocalDateTime.now())
                .note("Đơn hàng được khởi tạo")
                .build()
        );

        sendAsyncNotifications(userId, order, orderItems);

        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .code(order.getCode())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .items(orderItems.stream()
                        .map(oi -> OrderResponseDTO.OrderedLineDTO.builder()
                                .productName(oi.getProductName())
                                .sizeName(oi.getSizeName())
                                .quantity(oi.getQuantity())
                                .unitPrice(oi.getUnitPrice())
                                .toppingTotal(oi.getToppingTotal())
                                .lineTotal(oi.getLineTotal())
                                .note(oi.getNote())
                                .build())
                        .toList())
                .build();
    }

    @Async
    public void sendAsyncNotifications(Long userId, Order order, List<OrderItem> orderItems) {
        safe(() -> emailService.sendOrderConfirmationEmail(userId, order, orderItems));
        safe(() -> notificationService.create(
                userId,
                "ORDER",
                "Đặt hàng thành công",
                "Đơn hàng #" + order.getCode() + " đã được tạo thành công.",
                "Order",
                order.getId()
        ));
    }

    private String normalizePaymentMethod(String pm) {
        if (pm == null) return PaymentMethod.COD.name();
        try {
            return PaymentMethod.valueOf(pm.toUpperCase()).name();
        } catch (Exception e) {
            return PaymentMethod.COD.name();
        }
    }

    private String generateOrderCode() {
        String ymd = java.time.LocalDate.now().toString().replaceAll("-", "");
        String rand = String.valueOf((int) (Math.random() * 90000) + 10000);
        return "ALO-" + ymd + "-" + rand;
    }

    private void safe(Runnable r) {
        try {
            r.run();
        } catch (Exception ignored) {
        }
    }

    @Transactional(readOnly = true)
    public Order findOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional
    public void updateOrderStatusByCode(String code, String status, String note) {
        Order order = orderRepository.findByCode(code).orElse(null);
        if (order != null) {
            order.setStatus(status);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .status(status)
                    .changedAt(LocalDateTime.now())
                    .note(note)
                    .build());

            sendAsyncNotifications(order.getUserId(), order);
        }
    }

    @Async
    public void sendAsyncNotifications(Long userId, Order order) {
        safe(() -> {
            if (OrderStatus.PAID.name().equals(order.getStatus())) {
                emailService.sendPaymentSuccessEmail(userId, order);
            } else if (OrderStatus.FAILED.name().equals(order.getStatus())) {
                emailService.sendPaymentFailedEmail(userId, order);
            }
        });

        String vnStatus = getStatusLabel(order.getStatus());

        safe(() -> notificationService.create(
                userId,
                "ORDER",
                "Cập nhật trạng thái đơn hàng",
                String.format("Đơn hàng #%s đã được cập nhật trạng thái: %s", order.getCode(), vnStatus),
                "Order",
                order.getId()
        ));
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "PENDING" -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "SHIPPING" -> "Đang giao";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELED" -> "Đã hủy";
            case "PAID" -> "Thanh toán thành công";
            case "FAILED" -> "Thanh toán thất bại";
            default -> status;
        };
    }


    // ====================================
    // 🧾 2. LỊCH SỬ & CHI TIẾT ĐƠN HÀNG
    // ====================================

    private OrderDTO convertToOrderDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .code(order.getCode())
                .total(order.getTotal())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(Long userId, String status) {
        List<Order> orders;

        // 🔸 Lọc theo trạng thái nếu có
        if (status != null && !status.isBlank()) {
            orders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        // 🔸 Map sang DTO
        return orders.stream()
                .map((Order order) -> {
                    List<OrderItemDTO> items = orderItemRepository.findByOrderId(order.getId())
                            .stream()
                            .map((OrderItem item) -> OrderItemDTO.builder()
                                    .id(item.getId())
                                    .productName(item.getProductName())
                                    .sizeName(item.getSizeName())
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .toppingTotal(item.getToppingTotal())
                                    .lineTotal(item.getLineTotal())
                                    .note(item.getNote())
                                    .build())
                            .collect(Collectors.toList());

                    return OrderDTO.builder()
                            .id(order.getId())
                            .code(order.getCode())
                            .createdAt(order.getCreatedAt())
                            .status(order.getStatus())
                            .total(order.getTotal())
                            .paymentMethod(order.getPaymentMethod())
                            .items(items)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderItemDTO> getOrderItems(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập đơn hàng này");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        return items.stream()
                .map(item -> {
                    BigDecimal topping = item.getToppingTotal() != null ? item.getToppingTotal() : BigDecimal.ZERO;
                    return OrderItemDTO.builder()
                            .id(item.getId())
                            .productName(item.getProductName())
                            .sizeName(item.getSizeName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .toppingTotal(topping)
                            .lineTotal(item.getLineTotal())
                            .note(item.getNote())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new RuntimeException("Không thể hủy đơn hàng vì trạng thái không còn là PENDING");
        }

        // Cập nhật trạng thái đơn hàng
        order.setStatus(OrderStatus.CANCELED.name());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Ghi lại lịch sử trạng thái
        orderStatusHistoryRepository.save(
                OrderStatusHistory.builder()
                        .order(order)
                        .status(OrderStatus.CANCELED.name())
                        .changedAt(LocalDateTime.now())
                        .note("Người dùng đã hủy đơn hàng")
                        .build()
        );

        // Gửi thông báo
        sendAsyncNotifications(order.getUserId(), order);
    }


    @Transactional(readOnly = true)
    public OrderDetailDTO getOrderDetail(Long userId, Long orderId) {
        // 🧭 1. Xác thực quyền truy cập đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));



        // 🧃 2. Lấy danh sách sản phẩm & topping
        List<OrderItemDTO> itemDTOs = orderItemRepository.findByOrderId(orderId)
                .stream()
                .map(item -> {
                    // 🧋 2.1. Lấy topping của sản phẩm
                    List<ToppingDTO> toppingDTOs = orderItemToppingRepository.findByOrderItemId(item.getId())
                            .stream()
                            .map(t -> ToppingDTO.builder()
                                    .toppingName(t.getToppingName())
                                    .priceAtAddition(t.getPriceAtAddition())
                                    .build())
                            .collect(Collectors.toList());

                    // 🛍️ 2.2. Map sang DTO sản phẩm
                    return OrderItemDTO.builder()
                            .id(item.getId())
                            .productName(item.getProductName())
                            .sizeName(item.getSizeName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .toppingTotal(item.getToppingTotal())
                            .lineTotal(item.getLineTotal())
                            .note(item.getNote())
                            .toppings(toppingDTOs)
                            .build();
                })
                .collect(Collectors.toList());

        // 🕓 3. Lấy lịch sử trạng thái đơn hàng
        List<OrderStatusHistoryDTO> historyDTOs = orderStatusHistoryRepository
                .findByOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(h -> OrderStatusHistoryDTO.builder()
                        .status(h.getStatus())
                        .changedAt(h.getChangedAt())
                        .note(h.getNote())
                        .build())
                .collect(Collectors.toList()); // ⚡ dùng Collectors để tránh lỗi type inference

        // 📦 4. Trả về DTO chi tiết đơn hàng
        return OrderDetailDTO.builder()
                .id(order.getId())
                .code(order.getCode())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .paymentMethod(order.getPaymentMethod())
                .deliveryAddress(order.getDeliveryAddress())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(itemDTOs)
                .statusHistory(historyDTOs)
                .build();
    }


 // ====================================
    // 🧾 3. QUẢN LÝ ĐƠN HÀNG VENDOR
    // ====================================

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByVendor(Long vendorId, String status) {
        List<Order> orders;

        // ✅ Gọi repository JPQL thay vì method cũ
        if (status == null || status.isBlank()) {
            orders = orderRepository.findOrdersByVendorId(vendorId);
        } else {
            orders = orderRepository.findOrdersByVendorIdAndStatus(vendorId, status);
        }

        // ✅ Map sang DTO
        return orders.stream()
                .map(order -> {
                    List<OrderItemDTO> items = orderItemRepository.findByOrderId(order.getId())
                            .stream()
                            .map(item -> OrderItemDTO.builder()
                                    .id(item.getId())
                                    .productName(item.getProductName())
                                    .sizeName(item.getSizeName())
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .toppingTotal(item.getToppingTotal())
                                    .lineTotal(item.getLineTotal())
                                    .note(item.getNote())
                                    .build())
                            .toList();

                    return OrderDTO.builder()
                            .id(order.getId())
                            .code(order.getCode())
                            .createdAt(order.getCreatedAt())
                            .status(order.getStatus())
                            .total(order.getTotal())
                            .paymentMethod(order.getPaymentMethod())
                            .items(items)
                            .build();
                })
                .toList();
    }


    @Transactional
    public void confirmOrderByVendor(Long orderId, Long vendorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!branchService.isVendorOfBranch(vendorId, order.getBranchId())) {
            throw new RuntimeException("Bạn không có quyền thao tác đơn hàng này");
        }

        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể duyệt đơn khi trạng thái là PENDING");
        }

        order.setStatus(OrderStatus.CONFIRMED.name());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        orderStatusHistoryRepository.save(
                OrderStatusHistory.builder()
                        .order(order)
                        .status(OrderStatus.CONFIRMED.name())
                        .changedAt(LocalDateTime.now())
                        .note("Đơn hàng được xác nhận bởi cửa hàng")
                        .build()
        );

        sendAsyncNotifications(order.getUserId(), order);
    }



    @Transactional
    public void cancelOrderByVendor(Long orderId, Long vendorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!branchService.isVendorOfBranch(vendorId, order.getBranchId())) {
            throw new RuntimeException("Bạn không có quyền thao tác đơn hàng này");
        }

        if (!OrderStatus.PENDING.name().equals(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể hủy đơn khi trạng thái là PENDING");
        }

        order.setStatus(OrderStatus.CANCELED.name());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        orderStatusHistoryRepository.save(
                OrderStatusHistory.builder()
                        .order(order)
                        .status(OrderStatus.CANCELED.name())
                        .changedAt(LocalDateTime.now())
                        .note("Đơn hàng bị hủy bởi cửa hàng")
                        .build()
        );

        sendAsyncNotifications(order.getUserId(), order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersForAdmin(Long branchId, String status) {
        List<Order> orders;

        if (branchId != null && status != null) {
            orders = orderRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, status);
        } else if (branchId != null) {
            orders = orderRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
        } else if (status != null) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }

        return orders.stream().map(this::mapToOrderDTO).toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailDTO getOrderDetailForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        return mapToOrderDetailDTO(order);
    }

    private OrderDTO mapToOrderDTO(Order order) {
        String branchName = null;
        if (order.getBranchId() != null) {
            branchName = branchRepository.findById(order.getBranchId())
                    .map(Branch::getName)
                    .orElse(null);
        }

        return OrderDTO.builder()
                .id(order.getId())
                .code(order.getCode())
                .total(order.getTotal())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .branchName(branchName)               // ✅ lấy tên chi nhánh qua branchId
                .paymentMethod(order.getPaymentMethod())
                .build();
    }


    private OrderDetailDTO mapToOrderDetailDTO(Order order) {
        String branchName = null;
        if (order.getBranchId() != null) {
            branchName = branchRepository.findById(order.getBranchId())
                    .map(Branch::getName)
                    .orElse(null);
        }

        List<OrderItemDTO> itemDTOs = orderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(item -> OrderItemDTO.builder()
                        .id(item.getId())
                        .productName(item.getProductName())
                        .sizeName(item.getSizeName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .toppingTotal(item.getToppingTotal())
                        .lineTotal(item.getLineTotal())
                        .note(item.getNote())
                        .build())
                .toList();

        List<OrderStatusHistoryDTO> historyDTOs = orderStatusHistoryRepository
                .findByOrderIdOrderByChangedAtAsc(order.getId())
                .stream()
                .map(h -> OrderStatusHistoryDTO.builder()
                        .status(h.getStatus())
                        .changedAt(h.getChangedAt())
                        .note(h.getNote())
                        .build())
                .toList();

        return OrderDetailDTO.builder()
                .id(order.getId())
                .code(order.getCode())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .deliveryAddress(order.getDeliveryAddress())
                .createdAt(order.getCreatedAt())
                .branchName(branchName)                // ✅ lấy tên chi nhánh qua branchId
                .items(itemDTOs)
                .statusHistory(historyDTOs)
                .build();
    }


    @Transactional
    public void shipOrderByVendor(Long orderId, Long vendorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setStatus("SHIPPING");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 🧭 Lấy tất cả shipper thuộc chi nhánh
        List<Shipper> shippers = shipperRepository.findByCarrierIdAndIsDeletedFalse(order.getShippingCarrierId());
        for (Shipper s : shippers) {
            ShippingAssignment assignment = new ShippingAssignment();
            assignment.setOrderId(order.getId());
            assignment.setShipperId(s.getId());
            assignment.setStatus("PENDING");
            assignment.setAssignedAt(LocalDateTime.now());
            shippingAssignmentRepository.save(assignment);
        }

        // 📩 Gửi thông báo đến các shipper
        shippers.forEach(s -> notificationService.create(
              s.getUser().getId(),
                "ORDER",
                "Có đơn hàng mới",
                "Bạn có một đơn hàng mới #" + order.getCode() + " cần giao",
                "Order",
                order.getId()
        ));
    }
}
