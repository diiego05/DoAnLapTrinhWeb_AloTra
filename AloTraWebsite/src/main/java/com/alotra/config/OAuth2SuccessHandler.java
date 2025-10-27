package com.alotra.config;

import com.alotra.entity.User;
import com.alotra.entity.Role;
import com.alotra.repository.UserRepository;
import com.alotra.repository.RoleRepository;
import com.alotra.security.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // Kiểm tra nếu user đã tồn tại
        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Cập nhật thông tin nếu có
            if (name != null && !name.isEmpty()) {
                user.setFullName(name);
            }
            if (picture != null && !picture.isEmpty()) {
                // Lưu chung ảnh vào avatarUrl (không tạo cột mới)
                user.setAvatarUrl(picture);
            }
        } else {
            // Tạo user mới từ Google
            user = new User();
            user.setEmail(email);
            user.setFullName(name != null ? name : "Google User");
            // Lưu chung ảnh vào avatarUrl
            user.setAvatarUrl(picture);

            // Nếu không có phone, tạo phone giả duy nhất để thỏa ràng buộc @NotBlank và unique
            // Generate a short unique phone-like string to avoid DB column length issues
            String shortId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10);
            String generatedPhone = "g-" + shortId; // e.g. g-6ecaea7abb
            user.setPhone(generatedPhone);

            // Tạo mật khẩu ngẫu nhiên mạnh, mã hóa và lưu vào passwordHash
            String rawPwd = generateStrongPassword();
            user.setPasswordHash(passwordEncoder.encode(rawPwd));
            // Đánh dấu email đã xác thực
            user.setEmailVerifiedAt(LocalDateTime.now());
            // Đặt trạng thái active để có thể đăng nhập
            user.setStatus("ACTIVE");

            // Gán role mặc định là USER
            Role userRole = roleRepository.findByCode("USER")
                    .orElseThrow(() -> new IllegalStateException("Role USER không tồn tại"));
            user.setRole(userRole);
        }

        user = userRepository.save(user);

        // Tạo JWT token
        String token = jwtService.generate(
            email,
            Map.of(
                "id", user.getId(),
                "role", user.getRole().getCode(),
                "name", user.getFullName()
            )
        );

        // Thêm JWT vào cookie
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false) // true nếu dùng HTTPS
        .path("/alotra-website")
                .maxAge(86400) // 1 ngày
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

    // Thêm thêm 1 cookie không httpOnly để frontend có thể copy vào localStorage (tương thích với code frontend hiện tại)
    ResponseCookie clientCookie = ResponseCookie.from("jwtToken", token)
        .httpOnly(false)
        .secure(false)
        .path("/")
        .maxAge(86400)
        .build();
    response.addHeader("Set-Cookie", clientCookie.toString());

        // Redirect về trang home hoặc dashboard
        String redirectUrl = "/alotra-website/";
        if ("ADMIN".equals(user.getRole().getCode())) {
            redirectUrl = "/alotra-website/admin/dashboard";
        } else if ("VENDOR".equals(user.getRole().getCode())) {
            redirectUrl = "/alotra-website/vendor/dashboard";
        } else if ("SHIPPER".equals(user.getRole().getCode())) {
            redirectUrl = "/alotra-website/shipper/dashboard";
        }

        response.sendRedirect(redirectUrl);
    }

    /**
     * Sinh một mật khẩu mạnh thỏa validator: ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt
     */
    private String generateStrongPassword() {
        String upper = "A";
        String lower = "a";
        String digit = "0";
        String special = "!";
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String base = upper + lower + digit + special + uuid;
        // đảm bảo độ dài
        if (base.length() < 12) {
            base = base + "Ab1!Ab1!";
        }
        return base.substring(0, Math.max(12, base.length()));
    }
}
