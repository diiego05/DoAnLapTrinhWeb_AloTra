package com.alotra.controller.api;

import com.alotra.dto.NotificationDTO;
import com.alotra.entity.Notification;
import com.alotra.entity.User;
import com.alotra.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) return user.getId();
        try {
            var method = principal.getClass().getMethod("getId");
            Object id = method.invoke(principal);
            return id instanceof Long ? (Long) id : null;
        } catch (Exception e) {
            return null;
        }
    }

    // 📩 Lấy danh sách thông báo (DTO gọn nhẹ)
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getAll(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) return ResponseEntity.status(401).build();

        List<NotificationDTO> list = service.getUserNotifications(userId)
                .stream()
                .map(n -> new NotificationDTO(
                        n.getId(),
                        n.getTitle(),
                        n.getMessage(),
                        n.getType(),
                        n.getIsRead(),
                        n.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(list);
    }

    // 🔔 Đếm chưa đọc
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) return ResponseEntity.status(401).build();
        long unread = service.countUnread(userId);
        return ResponseEntity.ok(Map.of("unread", unread));
    }

    // ✅ Đánh dấu đã đọc
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Boolean>> markAsRead(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) return ResponseEntity.status(401).build();
        service.markAsRead(id);
        return ResponseEntity.ok(Map.of("isRead", true));
    }

    // 🆕 Chi tiết thông báo
    @GetMapping("/{id}")
    public ResponseEntity<NotificationDTO> getDetail(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) return ResponseEntity.status(401).build();

        Notification n = service.getNotificationById(id);
        if (n == null) return ResponseEntity.status(404).build();
        if (!n.getUser().getId().equals(userId))
            return ResponseEntity.status(403).build();

        NotificationDTO dto = new NotificationDTO(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getIsRead(),
                n.getCreatedAt()
        );

        return ResponseEntity.ok(dto);
    }

    // ❌ Xóa 1 thông báo
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) return ResponseEntity.status(401).build();

        Notification n = service.getNotificationById(id);
        if (n == null) return ResponseEntity.notFound().build();
        if (!n.getUser().getId().equals(userId))
            return ResponseEntity.status(403).build();

        service.deleteNotification(n.getUser(), id);
        return ResponseEntity.noContent().build();
    }

    // 🧹 Xóa tất cả thông báo
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAll(Authentication auth) {
        Long userId = extractUserId(auth);
        if (userId == null) return ResponseEntity.status(401).build();

        User user = new User(userId);
        service.clearAllNotifications(user);
        return ResponseEntity.noContent().build();
    }
}
