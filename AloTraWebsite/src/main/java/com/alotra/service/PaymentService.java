package com.alotra.service;

import com.alotra.entity.Order;
import com.alotra.entity.Payment;
import com.alotra.enums.PaymentStatus;
import com.alotra.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 🧾 Tạo bản ghi thanh toán khi checkout đơn hàng
     */
    @Transactional
    public Payment createPayment(Long orderId, String gateway, BigDecimal amount, String method) {
        Order orderRef = new Order();
        orderRef.setId(orderId);

        Payment payment = Payment.builder()
                .order(orderRef)
                .gateway(gateway)
                .paymentMethod(method)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .refundStatus("NONE")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }

    /**
     * 💰 Đánh dấu thanh toán thành công
     */
    @Transactional
    public void markSuccess(String transactionCode) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch thanh toán"));

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    /**
     * ❌ Đánh dấu thanh toán thất bại
     */
    @Transactional
    public void markFailed(String transactionCode, String rawResponse) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch thanh toán"));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setRawResponse(rawResponse);
        payment.setRetryCount(payment.getRetryCount() + 1); // 🔁 tăng retry khi thất bại
        paymentRepository.save(payment);
    }

    /**
     * 🏷️ Gán transaction code sau khi gọi API thanh toán
     */
    @Transactional
    public void updateTransactionCode(Long paymentId, String transactionCode) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        payment.setTransactionCode(transactionCode);
        paymentRepository.save(payment);
    }

    /**
     * 🪙 Cập nhật trạng thái hoàn tiền
     */
    @Transactional
    public void markRefunded(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        payment.setRefundStatus("REFUNDED");
        paymentRepository.save(payment);
    }

    @Transactional
    public void markRefundFailed(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        payment.setRefundStatus("FAILED");
        paymentRepository.save(payment);
    }

    /**
     * 📦 Lấy tất cả payment theo orderId
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * 📌 Lấy payment mới nhất theo orderId (phục vụ gửi mail, thông báo)
     */
    @Transactional(readOnly = true)
    public Payment getLatestPaymentByOrderId(Long orderId) {
        return paymentRepository.findTopByOrderIdOrderByPaidAtDesc(orderId).orElse(null);
    }

    /**
     * 🔁 Tăng retry count thủ công (nếu cần retry giao dịch)
     */
    @Transactional
    public void incrementRetryCount(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
        payment.setRetryCount(payment.getRetryCount() + 1);
        paymentRepository.save(payment);
    }
}
