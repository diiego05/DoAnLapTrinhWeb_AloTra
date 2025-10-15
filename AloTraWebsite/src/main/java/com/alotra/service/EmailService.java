package com.alotra.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.alotra.entity.Order;
import com.alotra.entity.OrderItem;
import com.alotra.entity.User;
import com.alotra.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private UserRepository userRepository; //

    // === 1️⃣ GỬI MAIL XÁC NHẬN ĐĂNG KÝ ===
    public void sendRegistrationOtpEmail(String to, String otp) {
        String subject = "[AloTra] Mã OTP xác thực tài khoản của bạn";
        String content = """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #007bff;">Xin chào,</h2>
                    <p>Cảm ơn bạn đã đăng ký tài khoản tại <strong>AloTra</strong>.</p>
                    <p>Mã OTP của bạn là:</p>
                    <h1 style="background: #f2f2f2; padding: 12px 20px; display: inline-block; border-radius: 6px; color: #007bff;">
                        %s
                    </h1>
                    <p>Mã OTP này sẽ hết hạn sau <strong>5 phút</strong>.</p>
                    <p style="margin-top: 24px;">Trân trọng,<br><strong>Đội ngũ AloTra</strong></p>
                    <hr style="margin-top:20px;">
                    <p style="font-size:13px; color:#888;">Email này được gửi tự động, vui lòng không trả lời.</p>
                </div>
                """.formatted(otp);

        sendHtmlMail(to, subject, content, "AloTra Verification");
    }

    // === 2️⃣ GỬI MAIL QUÊN MẬT KHẨU ===
    public void sendPasswordResetOtpEmail(String to, String otp) {
        String subject = "[AloTra] Xác nhận đặt lại mật khẩu của bạn";
        String content = """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #d9534f;">Xin chào,</h2>
                    <p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản tại <strong>AloTra</strong>.</p>
                    <p>Mã OTP của bạn là:</p>
                    <h1 style="background: #f8f9fa; padding: 12px 20px; display: inline-block; border-radius: 6px; color: #d9534f;">
                        %s
                    </h1>
                    <p>Mã này có hiệu lực trong <strong>5 phút</strong>. Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>
                    <p style="margin-top: 24px;">Trân trọng,<br><strong>Đội ngũ AloTra</strong></p>
                    <hr style="margin-top:20px;">
                    <p style="font-size:13px; color:#888;">Email này được gửi tự động, vui lòng không trả lời.</p>
                </div>
                """.formatted(otp);

        sendHtmlMail(to, subject, content, "AloTra Support");
    }

    // === HÀM DÙNG CHUNG GỬI HTML MAIL ===
    private void sendHtmlMail(String to, String subject, String htmlContent, String senderName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // ✅ Bọc phần này trong try-catch để tránh UnsupportedEncodingException
            try {
                helper.setFrom("tinthanhn81@gmail.com", senderName);
            } catch (java.io.UnsupportedEncodingException e) {
                helper.setFrom("tinthanhn81@gmail.com"); // fallback nếu lỗi
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("❌ Lỗi khi gửi email đến " + to + ": " + e.getMessage());
        }
    }


    public void sendOrderConfirmationEmail(Long userId, Order order, List<OrderItem> items) {
        // 👇 Trong thực tế, bạn nên lấy email người dùng từ UserService hoặc Order
        // Ở đây minh họa lấy từ UserService (có thể chỉnh lại tùy hệ thống của bạn)
        String to = getUserEmailById(userId); // 📌 cần implement hoặc gọi service của bạn

        String subject = "[AloTra] Xác nhận đơn hàng #" + order.getCode();

        StringBuilder itemHtml = new StringBuilder();
        for (OrderItem item : items) {
            BigDecimal unit = item.getUnitPrice().add(item.getToppingTotal());
            BigDecimal line = unit.multiply(BigDecimal.valueOf(item.getQuantity()));
            itemHtml.append("""
                <tr>
                    <td>%s (%s)</td>
                    <td style="text-align:center;">%d</td>
                    <td style="text-align:right;">%s</td>
                    <td style="text-align:right;">%s</td>
                </tr>
            """.formatted(
                    item.getProductName(),
                    item.getSizeName(),
                    item.getQuantity(),
                    formatCurrency(unit),
                    formatCurrency(line)
            ));
        }

        String content = """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color:#28a745;">Đơn hàng của bạn đã được tạo thành công ✅</h2>
                    <p>Cảm ơn bạn đã đặt hàng tại <strong>AloTra</strong>.</p>
                    <p><strong>Mã đơn hàng:</strong> %s</p>
                    <p><strong>Địa chỉ giao hàng:</strong> %s</p>
                    <p><strong>Phương thức thanh toán:</strong> %s</p>

                    <table style="width:100%%;border-collapse:collapse;margin-top:16px;">
                        <thead>
                            <tr style="background:#f8f9fa;">
                                <th style="text-align:left;padding:8px;">Sản phẩm</th>
                                <th style="text-align:center;padding:8px;">SL</th>
                                <th style="text-align:right;padding:8px;">Đơn giá</th>
                                <th style="text-align:right;padding:8px;">Thành tiền</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>

                    <hr style="margin:20px 0;">
                    <p><strong>Tạm tính:</strong> %s</p>
                    <p><strong>Giảm giá:</strong> -%s</p>
                    <p><strong>Phí vận chuyển:</strong> %s</p>
                    <h3 style="color:#d9534f;">Tổng cộng: %s</h3>

                    <p style="margin-top: 24px;">Trân trọng,<br><strong>Đội ngũ AloTra</strong></p>
                    <hr style="margin-top:20px;">
                    <p style="font-size:13px; color:#888;">Email này được gửi tự động, vui lòng không trả lời.</p>
                </div>
                """.formatted(
                order.getCode(),
                order.getDeliveryAddress(),
                order.getPaymentMethod(),
                itemHtml,
                formatCurrency(order.getSubtotal()),
                formatCurrency(order.getDiscount()),
                formatCurrency(order.getShippingFee()),
                formatCurrency(order.getTotal())
        );

        sendHtmlMail(to, subject, content, "AloTra Order");
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return String.format("%,.0f ₫", amount);
    }

    // 📌 Tùy hệ thống — có thể dùng UserService hoặc lấy trực tiếp từ Order
    private String getUserEmailById(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        return userOpt.map(User::getEmail).orElse(null);
    }}
