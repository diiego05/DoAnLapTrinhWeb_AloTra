package com.alotra.controller.api;

import com.alotra.dto.LoginRequest;
import com.alotra.dto.RegisterRequest;
import com.alotra.dto.JwtResponse;
import com.alotra.entity.User;
import com.alotra.repository.UserRepository;
import com.alotra.security.JwtService;
import com.alotra.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        try {
            authService.registerUser(
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                request.getPassword()
            );
            return ResponseEntity.ok(Map.of(
                "message", "✅ Đã gửi mã OTP đến email của bạn. Vui lòng xác thực tài khoản."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest request) {
        try {
            authService.verifyOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(Map.of(
                "message", "✅ Xác thực tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody EmailRequest request) {
        try {
            authService.sendPasswordResetOtp(request.getEmail());
            return ResponseEntity.ok(Map.of(
                "message", "📩 Đã gửi mã OTP đặt lại mật khẩu đến email của bạn."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok(Map.of(
                "message", "✅ Đặt lại mật khẩu thành công! Hãy đăng nhập với mật khẩu mới."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

            String token = jwtService.generate(
                user.getEmail(),
                Map.of("id", user.getId(),
                       "role", user.getRole().getCode(),
                       "name", user.getFullName())
            );

            // ✅ Thêm cookie JWT (tự gửi khi truy cập /admin/**)
            ResponseCookie cookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(false) // true nếu dùng HTTPS
                    .path("/alotra-website") // ✅ rất quan trọng (context-path)
                    .maxAge(86400) // 1 ngày
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());

            return ResponseEntity.ok(Map.of(
                "token", token,
                "role", user.getRole().getCode(),
                "fullName", user.getFullName()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Sai email hoặc mật khẩu"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }


    // ===================== DTOs phụ =====================
    public static class OtpRequest {
        private String email;
        private String otp;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }

    public static class EmailRequest {
        private String email;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        private String email;
        private String otp;
        private String newPassword;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
