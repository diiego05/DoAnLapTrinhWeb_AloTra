package com.alotra.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PromotionalCampaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionalCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String slug;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    private String bannerUrl;
    private Integer viewCount;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status;

    @Enumerated(EnumType.STRING)
    private CampaignType type;

    @Column(precision = 18, scale = 2)
    private BigDecimal value;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        viewCount = 0;
        if (status == null) status = CampaignStatus.SCHEDULED;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CampaignStatus {
        ACTIVE, EXPIRED, SCHEDULED
    }

    public enum CampaignType {
        ORDER_PERCENT,
        ORDER_FIXED,
        SHIPPING_PERCENT,
        SHIPPING_FIXED
    }
}
