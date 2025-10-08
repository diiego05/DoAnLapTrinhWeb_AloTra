package com.alotra.service;

import com.alotra.entity.OtpCode;
import com.alotra.entity.Role;
import com.alotra.entity.User;
import com.alotra.repository.OtpCodeRepository;
import com.alotra.repository.RoleRepository;
import com.alotra.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private OtpCodeRepository otpCodeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    @Transactional
    public void registerUser(String fullName, String email, String phone, String password)
 {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email đã được sử dụng!");
        }
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalStateException("Số điện thoại đã được sử dụng!");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password)); // Mã hóa mật khẩu
        Role userRole = roleRepository.findByCode("USER").orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy vai trò USER."));
        user.setRole(userRole);
        user.setStatus("INACTIVE");
        User savedUser = userRepository.save(user);

        String otp = String.format("%06d", new Random().nextInt(999999));
        OtpCode otpCode = new OtpCode();
        otpCode.setUser(savedUser);
        otpCode.setCode(otp);
        otpCode.setType("REGISTER");
        otpCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpCodeRepository.save(otpCode);
        emailService.sendRegistrationOtpEmail(email, otp);
    }

    @Transactional
    public void verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng."));
        OtpCode otpCode = otpCodeRepository.findValidOtp(user.getId(), otp, "REGISTER", LocalDateTime.now()).orElseThrow(() -> new IllegalStateException("Mã OTP không hợp lệ hoặc đã hết hạn."));
        user.setStatus("ACTIVE");
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        otpCode.setUsedAt(LocalDateTime.now());
        otpCodeRepository.save(otpCode);
    }
    @Transactional
    public void sendPasswordResetOtp(String email) {
        // Tìm user, nếu không tồn tại sẽ báo lỗi
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản nào với email này."));

        // Tạo mã OTP mới
        String otp = String.format("%06d", new Random().nextInt(999999));
        OtpCode otpCode = new OtpCode();
        otpCode.setUser(user);
        otpCode.setCode(otp);
        otpCode.setType("PASSWORD_RESET"); // Phân biệt với OTP đăng ký
        otpCode.setExpiresAt(LocalDateTime.now().plusMinutes(5)); // Hết hạn sau 5 phút
        otpCodeRepository.save(otpCode);

        // Gửi email
        emailService.sendPasswordResetOtpEmail(email, otp);
    }

    /**
     * BƯỚC 2: Xác thực OTP và đặt lại mật khẩu mới.
     */
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng."));

        // Tìm OTP hợp lệ cho việc reset password
        OtpCode otpCode = otpCodeRepository.findValidOtp(user.getId(), otp, "PASSWORD_RESET", LocalDateTime.now())
            .orElseThrow(() -> new IllegalStateException("Mã OTP không hợp lệ hoặc đã hết hạn."));

        // Cập nhật mật khẩu mới (đã được mã hóa)
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Đánh dấu OTP đã được sử dụng
        otpCode.setUsedAt(LocalDateTime.now());
        otpCodeRepository.save(otpCode);
    }
}