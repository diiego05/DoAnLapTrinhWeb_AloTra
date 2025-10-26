package com.alotra.controller.api;

import com.alotra.entity.Order;
import com.alotra.enums.PaymentStatus;
import com.alotra.service.OrderService;
import com.alotra.service.PaymentService;
import com.alotra.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
public class VnPayController {

    private final VnPayService vnPayService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * 🧾 1️⃣ Tạo link thanh toán VNPay cho đơn hàng
     */
    @PostMapping("/create")
    public ResponseEntity<String> createPayment(@RequestParam Long orderId, HttpServletRequest request) {
        Order order = orderService.findOrderById(orderId);
        if (order == null) {
            return ResponseEntity.badRequest().body("Đơn hàng không tồn tại");
        }

        // 💰 Tạo bản ghi thanh toán trước khi redirect sang VNPay
        var payment = paymentService.createPayment(
                order.getId(),
                "VNPAY",
                order.getTotal(),
                order.getPaymentMethod()
        );

        // 🔗 Sinh URL thanh toán từ VNPay
        String paymentUrl = vnPayService.createPaymentUrl(
                request,
                order.getTotal().longValue(),
                order.getCode()
        );

        // 🏷️ Gắn transactionCode vào payment (ở đây chính là orderCode hoặc response từ VNPay callback)
        paymentService.updateTransactionCode(payment.getId(), order.getCode());

        return ResponseEntity.ok(paymentUrl);
    }

    /**
     * 🪙 2️⃣ Xử lý callback từ VNPay
     * (Sau khi người dùng thanh toán xong)
     */
    @GetMapping("/return")
    public String handleReturn(@RequestParam Map<String, String> params) {
        boolean valid = vnPayService.validateSignature(params);
        String orderCode = params.get("vnp_TxnRef");
        String transactionStatus = params.get("vnp_TransactionStatus");

        if (valid && "00".equals(transactionStatus)) {
            // ✅ Cập nhật trạng thái thanh toán
            paymentService.markSuccess(orderCode);
            return "Thanh toán thành công đơn hàng #" + orderCode;
        } else {
            paymentService.markFailed(orderCode, params.toString());
            return "Thanh toán thất bại!";
        }
    }
}
