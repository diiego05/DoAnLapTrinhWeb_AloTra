package com.alotra.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "Shippers")
public class Shipper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CarrierId")
    private ShippingCarrier carrier;

    private String status;

    // 🆕 Thêm các trường này
    private String ward;
    private String district;
    private String city;

    private String vehicleType;
    private String vehiclePlate;

    @Column(length = 500)
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(name = "IsDeleted")
    private Boolean isDeleted = false;
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
