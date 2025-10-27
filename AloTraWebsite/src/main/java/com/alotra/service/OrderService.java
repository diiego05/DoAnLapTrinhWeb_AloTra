package com.alotra.service;

import com.alotra.dto.OrderDTO;
import com.alotra.dto.OrderDetailDTO;
import com.alotra.dto.OrderItemDTO;
import com.alotra.dto.OrderStatusHistoryDTO;
import com.alotra.dto.PaymentDTO;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

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

        // 🧭 Xác định chi nhánh: dùng branchId nếu truyền vào, nếu không -> tìm gần nhất theo tọa độ địa chỉ
        Long selectedBranchId = req.getBranchId();
        if (selectedBranchId == null) {
            selectedBranchId = findNearestActiveBranchId(userId, req.getAddressId());
            if (selectedBranchId == null) {
                throw new IllegalStateException("Không thể xác định chi nhánh gần nhất. Vui lòng chọn chi nhánh.");
            }
        }

        var unavailable = branchService.checkCartItemAvailability(selectedBranchId, req.getCartItemIds());
        if (!unavailable.isEmpty()) {
            throw new IllegalStateException("Một số sản phẩm không khả dụng tại chi nhánh này.");
        }

        // 🧮 Tính tổng phụ + topping
        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().add(i.getToppingTotalEach())
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🚚 Tính phí vận chuyển
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (req.getPaymentMethod() == null ||
                !req.getPaymentMethod().equalsIgnoreCase(PaymentMethod.PICKUP.name())) {
            var carrier = shippingCarrierService.findActiveById(req.getShippingCarrierId());
            shippingFee = carrier.getBaseFee();
        }

        // 🏷️ Áp dụng mã giảm giá
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

        // 🏡 Snapshot địa chỉ
        String deliveryAddress = addressService.snapshotAddress(req.getAddressId(), userId, req.getPaymentMethod());

        // 🧾 Tạo đơn hàng
        Order order = Order.builder()
                .code(generateOrderCode())
                .userId(userId)
                .branchId(selectedBranchId)
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

        // 💬 Lưu các item
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

        // 🧋 Lưu topping
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

        // 🛒 Xóa giỏ hàng
        cartService.removeItems(userId, req.getCartItemIds());

        if (couponId != null) couponService.increaseUsedCount(couponId);

        // 🕓 Lưu lịch sử trạng thái đơn hàng
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PENDING.name())
                .changedAt(LocalDateTime.now())
                .note("Đơn hàng được khởi tạo")
                .build()
        );

        // 💳 Tạo payment record
        paymentService.createPayment(
                order.getId(),
                req.getGateway() != null ? req.getGateway() : "COD",
                total,
                order.getPaymentMethod()
        );

        // 📩 Gửi thông báo
        sendAsyncNotifications(userId, order);

        // ✅ Trả về phản hồi
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

    private Long findNearestActiveBranchId(Long userId, Long addressId) {
        var opt = addressService.getCoordinates(userId, addressId);
        if (opt.isEmpty()) return null;
        double lat = opt.get().latitude();
        double lng = opt.get().longitude();
        return branchRepository.findByStatus("ACTIVE").stream()
                .filter(b -> b.getLatitude() != null && b.getLongitude() != null)
                .min(Comparator.comparingDouble(b -> haversineKm(lat, lng, b.getLatitude(), b.getLongitude())))
                .map(Branch::getId)
                .orElse(null);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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

        return orders.stream().map(order -> {
            // 🧾 Map danh sách sản phẩm
            List<OrderItemDTO> items = orderItemRepository.findByOrderId(order.getId())
                    .stream()
                    .map(item -> OrderItemDTO.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .variantId(item.getVariantId())
                            .productName(item.getProductName())
                            .sizeName(item.getSizeName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .toppingTotal(item.getToppingTotal())
                            .lineTotal(item.getLineTotal())
                            .note(item.getNote())
                            .build())
                    .collect(Collectors.toList());

            // 💳 Lấy bản ghi Payment mới nhất (nếu có nhiều)
            PaymentDTO paymentDTO = null;
            if (order.getPayments() != null && !order.getPayments().isEmpty()) {
                Payment latestPayment = order.getPayments().stream()
                        .max(Comparator.comparing(Payment::getCreatedAt)) // lấy payment mới nhất
                        .orElse(null);

                if (latestPayment != null) {
                    paymentDTO = PaymentDTO.builder()
                            .id(latestPayment.getId())
                            .gateway(latestPayment.getGateway())
                            .paymentMethod(latestPayment.getPaymentMethod())
                            .amount(latestPayment.getAmount())
                            .status(latestPayment.getStatus())                   // ✅ Enum
                            .createdAt(latestPayment.getCreatedAt())
                            .paidAt(latestPayment.getPaidAt())
                            .refundStatus(latestPayment.getRefundStatus())
                            .transactionCode(latestPayment.getTransactionCode()) // ✅ Mã giao dịch
                            .build();
                }
            }

            return OrderDTO.builder()
                    .id(order.getId())
                    .code(order.getCode())
                    .createdAt(order.getCreatedAt())
                    .status(order.getStatus())
                    .total(order.getTotal())
                    .paymentMethod(order.getPaymentMethod())
                    .items(items)
                    .payment(paymentDTO)
                    .build();
        }).collect(Collectors.toList());
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
                            .productId(item.getProductId())       // ✅
                            .variantId(item.getVariantId())
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

        // (Tùy chọn: nếu bạn muốn kiểm tra quyền truy cập người dùng)
        // if (!order.getUser().getId().equals(userId)) {
        //     throw new RuntimeException("Không có quyền truy cập đơn hàng này");
        // }

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
                            .productId(item.getProductId())
                            .variantId(item.getVariantId())
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
                .collect(Collectors.toList());

        // 💳 4. Lấy thông tin thanh toán mới nhất (nếu có)
        Optional<Payment> paymentOpt = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId);

        PaymentDTO paymentDTO = null;
        if (paymentOpt.isPresent()) {
            Payment p = paymentOpt.get();
            paymentDTO = PaymentDTO.builder()
                    .id(p.getId())
                    .gateway(p.getGateway())
                    .amount(p.getAmount())
                    .transactionCode(p.getTransactionCode())
                    .status(p.getStatus())       // ⚡ Trạng thái thanh toán
                    .paidAt(p.getPaidAt())
                    .createdAt(p.getCreatedAt())
                    .paymentMethod(p.getPaymentMethod())
                    .build();
        }

        // 📦 5. Trả về DTO chi tiết đơn hàng
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
                .payment(paymentDTO)  // ✅ thêm payment vào DTO
                .build();
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

        return orders.stream()
                .map(this::mapToOrderDTOWithPayment) // 🆕 map có thêm thông tin thanh toán
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailDTO getOrderDetailForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        return mapToOrderDetailDTOWithPayment(order);
    }

    private OrderDTO mapToOrderDTOWithPayment(Order order) {
        String branchName = null;
        if (order.getBranchId() != null) {
            branchName = branchRepository.findById(order.getBranchId())
                    .map(Branch::getName)
                    .orElse(null);
        }

        // 🔸 Lấy payment mới nhất
        var payment = paymentRepository.findTopByOrderIdOrderByPaidAtDesc(order.getId()).orElse(null);
        PaymentDTO paymentDTO = mapPaymentToDTO(payment);

        return OrderDTO.builder()
                .id(order.getId())
                .code(order.getCode())
                .total(order.getTotal())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .branchName(branchName)
                .paymentMethod(order.getPaymentMethod())
                .payment(paymentDTO) // 🆕 gắn vào DTO
                .build();
    }

    private OrderDetailDTO mapToOrderDetailDTOWithPayment(Order order) {
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
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
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

        // 🔸 Lấy payment mới nhất
        var payment = paymentRepository.findTopByOrderIdOrderByPaidAtDesc(order.getId()).orElse(null);
        PaymentDTO paymentDTO = mapPaymentToDTO(payment);

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
                .branchName(branchName)
                .items(itemDTOs)
                .statusHistory(historyDTOs)
                .payment(paymentDTO) // 🆕 gắn vào DTO
                .build();
    }

    private PaymentDTO mapPaymentToDTO(com.alotra.entity.Payment payment) {
        if (payment == null) return null;
        return PaymentDTO.builder()
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

 // ====================================
    // 🧾 3. QUẢN LÝ ĐƠN HÀNG VENDOR
    // ====================================

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByVendor(Long vendorId, String status, LocalDateTime from, LocalDateTime to, String keyword) {
        // ✅ Sử dụng truy vấn tổng hợp hỗ trợ lọc
        List<Order> orders = orderRepository.searchVendorOrders(vendorId, emptyToNull(status), from, to, emptyToNull(keyword));

        return orders.stream()
                .map(order -> {
                    List<OrderItemDTO> items = orderItemRepository.findByOrderId(order.getId())
                            .stream()
                            .map(item -> OrderItemDTO.builder()
                                    .id(item.getId())
                                    .productId(item.getProductId())
                                    .variantId(item.getVariantId())
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

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
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



    @Transactional
    public void shipOrderByVendor(Long orderId, Long vendorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setStatus("WAITING_FOR_PICKUP");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 🧭 Lấy tất cả shipper APPROVED
        List<Shipper> shippers = shipperRepository
                .findByCarrierIdAndStatusAndIsDeletedFalse(order.getShippingCarrierId(), "APPROVED");

        for (Shipper s : shippers) {
            ShippingAssignment assignment = new ShippingAssignment();
            assignment.setOrderId(order.getId());
            assignment.setShipperId(s.getId());
            assignment.setStatus("PENDING");
            assignment.setAssignedAt(LocalDateTime.now());
            shippingAssignmentRepository.save(assignment);
        }

        shippers.forEach(s -> notificationService.create(
                s.getUser().getId(),
                "ORDER",
                "Có đơn hàng mới",
                "Bạn có một đơn hàng mới #" + order.getCode() + " cần giao",
                "Order",
                order.getId()
        ));
    }

    @Transactional(readOnly = true)
    public Order findOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    // Helpers
    private String generateOrderCode() {
        String ymd = java.time.LocalDate.now().toString().replaceAll("-", "");
        String rand = String.valueOf((int) (Math.random() * 90000) + 10000);
        return "ALO-" + ymd + "-" + rand;
    }

    private String normalizePaymentMethod(String pm) {
        if (pm == null) return PaymentMethod.COD.name();
        try {
            return PaymentMethod.valueOf(pm.toUpperCase()).name();
        } catch (Exception e) {
            return PaymentMethod.COD.name();
        }
    }

    @Async
    protected void sendAsyncNotifications(Long userId, Order order) {
        try {
            notificationService.create(
                    userId,
                    "ORDER",
                    "Cập nhật đơn hàng",
                    "Đơn hàng #" + order.getCode() + " hiện trạng thái: " + order.getStatus(),
                    "Order",
                    order.getId()
            );
        } catch (Exception ignore) {}

        try {
            if (OrderStatus.PENDING.name().equals(order.getStatus())) {
                var items = orderItemRepository.findByOrderId(order.getId());
                emailService.sendOrderConfirmationEmail(userId, order, items);
            }
        } catch (Exception ignore) {}
    }

    private void safe(Runnable r) {
        try { r.run(); } catch (Exception ignored) {}
    }
}