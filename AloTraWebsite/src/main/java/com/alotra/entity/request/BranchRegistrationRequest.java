package com.alotra.entity.request;

import com.alotra.entity.Branch;
import com.alotra.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "BranchRegistrationRequests")
@Getter
@Setter
public class BranchRegistrationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🧑 User gửi yêu cầu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User user;

    // Loại yêu cầu: CREATE hoặc JOIN
    @Enumerated(EnumType.STRING)
    @Column(name = "Type", nullable = false, length = 20)
    private BranchRequestType type;

    // Chi nhánh muốn tham gia (nếu JOIN)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchId")
    private Branch branch;

    // Thông tin chi nhánh đề xuất (nếu CREATE)
    @Column(name = "Name", length = 100)
    private String name;

    @Column(name = "Address", length = 255)
    private String address;

    @Column(name = "Phone", length = 20)
    private String phone;

    // 🗺️ Toạ độ địa lý của chi nhánh
    @Column(name = "Latitude")
    private Double latitude;

    @Column(name = "Longitude")
    private Double longitude;

    // Trạng thái yêu cầu
    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    // Ghi chú từ admin khi phê duyệt/từ chối
    @Column(name = "AdminNote", length = 255)
    private String adminNote;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}