package com.alotra.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CampaignId")
    private PromotionalCampaign campaign; // có thể null

    @Column(name = "Code", nullable = false, unique = true)
    private String code;

    @Column(name = "Type", nullable = false)
    private String type; // PERCENT, ORDER, SHIPPING...

    @Column(name = "Value", nullable = false)
    private BigDecimal value;

    @Column(name = "MaxDiscount")
    private BigDecimal maxDiscount;

    @Column(name = "MinOrderTotal")
    private BigDecimal minOrderTotal;

    @Column(name = "StartAt")
    private LocalDateTime startAt;

    @Column(name = "EndAt")
    private LocalDateTime endAt;

    @Column(name = "UsageLimit")
    private Integer usageLimit;

    @Column(name = "UsedCount", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "Status")
    private String status;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
