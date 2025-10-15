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

import com.alotra.dto.checkout.CheckoutRequestDTO;
import com.alotra.dto.checkout.OrderResponseDTO;
import com.alotra.entity.*;
import com.alotra.enums.OrderStatus;
import com.alotra.enums.PaymentMethod;
import com.alotra.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
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

    /**
     * ✅ Giai đoạn 1: xử lý transaction + lưu dữ liệu
     */
    @Transactional
    public OrderResponseDTO checkout(Long userId, CheckoutRequestDTO req) {
        // 1️⃣ Kiểm tra cart
        if (req.getCartItemIds() == null || req.getCartItemIds().isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm nào để thanh toán");
        }

        var items = cartService.getItemDetailsByIds(userId, req.getCartItemIds());
        if (items.isEmpty()) throw new IllegalArgumentException("Cart items không hợp lệ");

        // 2️⃣ Kiểm tra chi nhánh khả dụng
        var unavailable = branchService.checkCartItemAvailability(req.getBranchId(), req.getCartItemIds());
        if (!unavailable.isEmpty()) {
            throw new IllegalStateException("Một số sản phẩm không khả dụng tại chi nhánh này.");
        }

        // 3️⃣ Tính subtotal
        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().add(i.getToppingTotalEach())
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4️⃣ Tính phí ship
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (req.getPaymentMethod() == null ||
                !req.getPaymentMethod().equalsIgnoreCase(PaymentMethod.PICKUP.name())) {
            var carrier = shippingCarrierService.findActiveById(req.getShippingCarrierId());
            shippingFee = carrier.getBaseFee();
        }

        // 5️⃣ Áp dụng coupon
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

        // 6️⃣ Snapshot địa chỉ
        String deliveryAddress = addressService.snapshotAddress(req.getAddressId(), userId, req.getPaymentMethod());

        // 7️⃣ Lưu Order
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
        Order savedOrder = orderRepository.save(order);
        final Order finalOrder = savedOrder;
        // 8️⃣ Lưu OrderItem
        List<OrderItem> orderItems = items.stream().map(ci -> OrderItem.builder()
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
                .build()
        ).toList();
        orderItemRepository.saveAll(orderItems);

        // 9️⃣ Lưu topping (batch save)
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

        // 🔟 Xóa giỏ hàng
        cartService.removeItems(userId, req.getCartItemIds());

        // 1️⃣0️⃣ Cập nhật coupon usage
        if (couponId != null) couponService.increaseUsedCount(couponId);

        // 🕓 Lưu lịch sử trạng thái
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PENDING.name())
                .changedAt(LocalDateTime.now())
                .note("Đơn hàng được khởi tạo")
                .build()
        );

        // 🚀 Gửi mail + notification bất đồng bộ
        sendAsyncNotifications(userId, order, orderItems);

        // 1️⃣2️⃣ Trả về DTO
        return OrderResponseDTO.builder()
                .orderId(order.getId())
                .code(order.getCode())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
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

    /**
     * 🚀 Giai đoạn 2: xử lý tác vụ nặng bất đồng bộ
     */
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
        try { return PaymentMethod.valueOf(pm.toUpperCase()).name(); }
        catch (Exception e) { return PaymentMethod.COD.name(); }
    }

    private String generateOrderCode() {
        String ymd = java.time.LocalDate.now().toString().replaceAll("-", "");
        String rand = String.valueOf((int) (Math.random() * 90000) + 10000);
        return "ALO-" + ymd + "-" + rand;
    }

    private void safe(Runnable r) {
        try { r.run(); } catch (Exception ignored) {}
    }
}
