package com.alotra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromotionTargetDTO {
    private Long id;
    private Long campaignId;
    private String campaignName;
    private String targetType;
    private Long productId;
}
