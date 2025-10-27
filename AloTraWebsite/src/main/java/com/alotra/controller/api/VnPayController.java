package com.alotra.controller.api;

import com.alotra.entity.Order;
import com.alotra.service.OrderService;
import com.alotra.service.PaymentService;
import com.alotra.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
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
    @ResponseBody // 👈 chỉ phương thức này trả JSON
    public ResponseEntity<String> createPayment(@RequestParam Long orderId, HttpServletRequest request) {
        Order order = orderService.findOrderById(orderId);
        if (order == null) {
            return ResponseEntity.badRequest().body("Đơn hàng không tồn tại");
        }

        // 💰 Tạo bản ghi thanh toán
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

        // 🏷️ Gắn transactionCode vào payment
        paymentService.updateTransactionCode(payment.getId(), order.getCode());

        return ResponseEntity.ok(paymentUrl);
    }

    /**
     * 🪙 2️⃣ Xử lý callback từ VNPay
     */
    @GetMapping("/return")
    public String handleReturn(@RequestParam Map<String, String> params) {
        boolean valid = vnPayService.validateSignature(params);
        String orderCode = params.get("vnp_TxnRef");
        String status = params.get("vnp_TransactionStatus");

        if (valid && "00".equals(status)) {
            paymentService.markSuccess(orderCode);
        } else {
            paymentService.markFailed(orderCode, params.toString());
        }

        // ✅ Redirect thật sự về trang orders
        return "redirect:/orders";
    }
}
