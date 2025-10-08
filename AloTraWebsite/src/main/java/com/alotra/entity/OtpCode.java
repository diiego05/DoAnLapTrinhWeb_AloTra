package com.alotra.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "OtpCodes")
public class OtpCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "UserId", nullable = false)
    private User user;

    @Column(length = 10, nullable = false)
    private String code;

    @Column(length = 30, nullable = false)
    private String type;

    @Column(name = "ExpiresAt", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "UsedAt")
    private LocalDateTime usedAt;
}