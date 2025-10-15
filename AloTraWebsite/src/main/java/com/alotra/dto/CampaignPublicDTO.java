package com.alotra.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CampaignPublicDTO {
    private Long id;
    private String name;
    private String banner;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String description;

    private String targetType;
    private List<String> targetDetails;

    private Long viewCount;

    private List<CouponInfo> coupons;

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
    public static class CouponInfo {
        private Long id;
        private String code;
        private String type;
        private Double value;
    }
}
