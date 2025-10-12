package com.alotra.service;

import com.alotra.entity.Notification;
import com.alotra.entity.User;
import com.alotra.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository repo;

    @PersistenceContext
    private EntityManager em;

    // ==================== TẠO THÔNG BÁO MỚI ====================
    public void create(Long userId, String type, String title, String message,
                       String relatedEntity, Long entityId) {
        Notification n = new Notification();
        n.setUser(new User(userId));
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setRelatedEntity(relatedEntity);
        n.setRelatedEntityId(entityId);
        repo.save(n);
    }

    // ==================== LẤY DANH SÁCH THEO NGƯỜI DÙNG ====================
    public List<Notification> getUserNotifications(Long userId) {
        em.clear(); // đảm bảo lấy dữ liệu mới nhất
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ==================== ĐẾM SỐ THÔNG BÁO CHƯA ĐỌC ====================
    public long countUnread(Long userId) {
        em.clear();
        return repo.countByUserIdAndIsReadFalse(userId);
    }

    // ==================== ĐÁNH DẤU ĐÃ ĐỌC ====================
    public void markAsRead(Long id) {
        Notification n = repo.findById(id).orElseThrow();
        n.setIsRead(true);
        repo.saveAndFlush(n); // flush xuống DB ngay
        em.clear();           // clear cache để GET sau đọc đúng dữ liệu
    }

    // ==================== LẤY CHI TIẾT THEO ID ====================
    public Notification getNotificationById(Long id) {
        em.clear();
        return repo.findById(id).orElse(null);
    }

    // ==================== LƯU / CẬP NHẬT ====================
    public Notification save(Notification n) {
        return repo.saveAndFlush(n);
    }

    // ==================== ❌ XÓA MỘT THÔNG BÁO ====================
    public void deleteNotification(User user, Long id) {
        Notification notif = repo.findByIdAndUserId(id, user.getId())
                .orElse(null);
        if (notif != null) {
            repo.delete(notif);
            em.flush();
            em.clear();
        }
    }

    // ==================== 🗑️ XÓA TẤT CẢ THÔNG BÁO ====================
    public void clearAllNotifications(User user) {
        repo.deleteByUserId(user.getId());
        em.flush();
        em.clear();
    }
}
