package com.alotra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

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

}
