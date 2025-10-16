// com/alotra/controller/api/OrderApiController.java
package com.alotra.controller.api;

import com.alotra.dto.OrderDTO;
import com.alotra.dto.OrderDetailDTO;
import com.alotra.dto.cart.CartItemResponse;
import com.alotra.dto.cart.CartResponse;
import com.alotra.dto.checkout.CheckoutRequestDTO;
import com.alotra.dto.checkout.OrderResponseDTO;
import com.alotra.service.CartService;
import com.alotra.service.OrderService;
import com.alotra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderApiController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;

    // 🧾 1️⃣ Checkout từ cart
    @PostMapping
    public ResponseEntity<OrderResponseDTO> checkout(@RequestBody CheckoutRequestDTO req) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(orderService.checkout(userId, req));
    }

    // 🛍 2️⃣ Lấy toàn bộ item trong giỏ hàng hiện tại
    @GetMapping("/cart-items")
    public ResponseEntity<CartResponse> getCartItems() {
        Long userId = userService.getCurrentUserId();
        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }

    // 🧾 3️⃣ Lấy chi tiết item theo danh sách ID (để xác nhận checkout)
    @PostMapping("/cart-items/by-ids")
    public ResponseEntity<List<CartService.CartItemDetail>> getCartItemsByIds(@RequestBody List<Long> cartItemIds) {
        Long userId = userService.getCurrentUserId();
        List<CartService.CartItemDetail> details = cartService.getItemDetailsByIds(userId, cartItemIds);
        return ResponseEntity.ok(details);
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(@RequestParam(required = false) String status) {
        Long userId = userService.getCurrentUserId();
        List<OrderDTO> orders = orderService.getOrdersByStatus(userId, status);
        return ResponseEntity.ok(orders);
    }

    // 🧾 5️⃣ Lấy chi tiết đơn hàng
    // Ví dụ: GET /api/orders/123
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailDTO> getOrderDetail(@PathVariable Long orderId) {
        Long userId = userService.getCurrentUserId();
        OrderDetailDTO orderDetail = orderService.getOrderDetail(userId, orderId);
        return ResponseEntity.ok(orderDetail);
    }

 // 🧾 6️⃣ Lấy danh sách item của một đơn hàng cụ thể
    @GetMapping("/{orderId}/items")
    public ResponseEntity<?> getOrderItems(@PathVariable Long orderId) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(orderService.getOrderItems(userId, orderId));
    }





    // 🧾 5️⃣ Hủy đơn hàng nếu trạng thái hiện tại là PENDING
    // 👉 PUT /api/orders/{orderId}/cancel
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        Long userId = userService.getCurrentUserId();
        orderService.cancelOrder(userId, orderId);
        return ResponseEntity.ok().build();
    }

}
