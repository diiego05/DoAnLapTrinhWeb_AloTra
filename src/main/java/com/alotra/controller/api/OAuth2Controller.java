package com.alotra.controller.api;

import com.alotra.entity.User;
import com.alotra.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth2")
public class OAuth2Controller {

    @Autowired
    private UserRepository userRepository;

    /**
     * API để lấy thông tin user sau khi đăng nhập qua OAuth2
     */
    @GetMapping("/user")
    public ResponseEntity<?> getOAuth2User(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("email", oAuth2User.getAttribute("email"));
        response.put("name", oAuth2User.getAttribute("name"));
        response.put("picture", oAuth2User.getAttribute("picture"));
        response.put("attributes", oAuth2User.getAttributes());

        return ResponseEntity.ok(response);
    }

    /**
     * API để check xem email có tồn tại trong hệ thống không
     */
    @GetMapping("/check-email/{email}")
    public ResponseEntity<?> checkEmail(@PathVariable String email) {
        boolean exists = userRepository.findByEmail(email).isPresent();
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * API để lấy thông tin profile của user hiện tại
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        String email = oAuth2User.getAttribute("email");
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
    // Trả cả 2 key để tương thích: avatarUrl là chính, profileImage giữ lại giá trị cũ
    profile.put("avatarUrl", user.getAvatarUrl());
    profile.put("profileImage", user.getAvatarUrl());
        profile.put("role", user.getRole().getCode());
        profile.put("status", user.getStatus());

        return ResponseEntity.ok(profile);
    }
}
