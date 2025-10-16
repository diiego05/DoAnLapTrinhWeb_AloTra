package com.alotra.controller.api;

import com.alotra.enums.OrderStatus;
import com.alotra.service.OrderService;
import com.alotra.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
public class VnPayController {

    private final VnPayService vnPayService;
    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<String> createPayment(@RequestParam Long orderId, HttpServletRequest request) {
        var order = orderService.findOrderById(orderId);
        if (order == null) {
            return ResponseEntity.badRequest().body("Đơn hàng không tồn tại");
        }

        String paymentUrl = vnPayService.createPaymentUrl(request,
                order.getTotal().longValue(),
                order.getCode());

        return ResponseEntity.ok(paymentUrl);
    }

    @GetMapping("/return")
    public String handleReturn(@RequestParam Map<String,String> params) {
        boolean valid = vnPayService.validateSignature(params);
        String orderCode = params.get("vnp_TxnRef");
        String status = params.get("vnp_TransactionStatus");

        if (valid && "00".equals(status)) {
            orderService.updateOrderStatusByCode(orderCode, OrderStatus.PAID.name(), "Thanh toán VNPay thành công");
            return "Thanh toán thành công đơn hàng #" + orderCode;
        } else {
            orderService.updateOrderStatusByCode(orderCode, OrderStatus.AWAITING_PAYMENT.name(), "Thanh toán VNPay thất bại");
            return "Thanh toán thất bại!";
        }
    }

}
