package com.alotra.dto;

import lombok.Data;
import java.util.List;

@Data
public class CampaignTargetRequest {
    private String targetType;      // ALL_PRODUCTS | CATEGORY | PRODUCTS
    private Long categoryId;
    private List<Long> productIds;
}
