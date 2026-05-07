package com.alotra.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PromotionTargets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CampaignId")
    private PromotionalCampaign campaign;

    private Long productId;  // có thể mở rộng thêm kiểu khác sau này
    private String targetType; // PRODUCT, CATEGORY ...
}
