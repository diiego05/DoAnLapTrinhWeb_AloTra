package com.alotra.controller.api;

import com.alotra.entity.User;
import com.alotra.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // === LẤY THÔNG TIN NGƯỜI DÙNG HIỆN TẠI ===
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "Người dùng chưa đăng nhập hoặc token không hợp lệ."
            ));
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng."));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("role", user.getRole() != null ? user.getRole().getCode() : "UNKNOWN");
        response.put("status", user.getStatus());
        response.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "/alotra-website/images/default-avatar.png");
        response.put("emailVerifiedAt", user.getEmailVerifiedAt());
        response.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    // === LẤY THÔNG TIN NGƯỜI DÙNG THEO ID (CHỈ ADMIN) ===
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", user.getId());
                    data.put("email", user.getEmail());
                    data.put("fullName", user.getFullName());
                    data.put("role", user.getRole() != null ? user.getRole().getCode() : "UNKNOWN");
                    data.put("status", user.getStatus());
                    data.put("phone", user.getPhone());
                    data.put("avatarUrl", user.getAvatarUrl());
                    data.put("createdAt", user.getCreatedAt());
                    return ResponseEntity.ok(data);
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy người dùng.")));
    }
}
