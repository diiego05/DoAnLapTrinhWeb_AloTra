package com.alotra.service;

import com.alotra.dto.CampaignPublicDTO;
import com.alotra.entity.*;
import com.alotra.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignPublicService {

    private final PromotionalCampaignRepository campaignRepo;
    private final PromotionTargetRepository targetRepo;
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final CouponRepository couponRepo;

    @Transactional
    public CampaignPublicDTO getCampaignDetailAndIncreaseView(Long campaignId) {
        PromotionalCampaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chiến dịch"));

        // ✅ Tăng lượt xem
        campaignRepo.incrementViewCount(campaignId);
        campaign.setViewCount(campaign.getViewCount() + 1);

        // ✅ Lấy target
        List<PromotionTarget> targets = targetRepo.findByCampaignId(campaignId);
        String targetType = targets.isEmpty() ? "ALL_PRODUCTS" : targets.get(0).getTargetType();

        List<String> targetNames;
        switch (targetType) {
            case "CATEGORY" -> {
                Long categoryId = targets.get(0).getProductId();
                String categoryName = categoryRepo.findById(categoryId)
                        .map(Category::getName)
                        .orElse("Danh mục không xác định");
                targetNames = List.of(categoryName);
            }
            case "PRODUCTS" -> targetNames = targets.stream()
                    .map(t -> productRepo.findById(t.getProductId())
                            .map(Product::getName)
                            .orElse("Không xác định"))
                    .toList();
            default -> targetNames = List.of("Toàn bộ sản phẩm");
        }

        // ✅ Lấy coupon
        List<Coupon> coupons = couponRepo.findByCampaignId(campaignId);

        return CampaignPublicDTO.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .banner(campaign.getBannerUrl())
                .startAt(campaign.getStartAt())
                .endAt(campaign.getEndAt())
                .description(campaign.getDescription())
                .targetType(targetType)
                .targetDetails(targetNames)
                .viewCount(campaign.getViewCount().longValue())
                .coupons(coupons.stream().map(c ->
                        CampaignPublicDTO.CouponInfo.builder()
                                .id(c.getId())
                                .code(c.getCode())
                                .type(c.getType())
                                .value(c.getValue().doubleValue())
                                .build()).toList())
                .build();
    }
    public List<CampaignPublicDTO> getTopCampaigns() {
        List<PromotionalCampaign> campaigns = campaignRepo.findAll();

        return campaigns.stream()
                .sorted(Comparator.comparingInt(PromotionalCampaign::getViewCount).reversed())
                .limit(6) // giới hạn top 6
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private CampaignPublicDTO convertToDTO(PromotionalCampaign c) {
        return CampaignPublicDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .banner(c.getBannerUrl())
                .startAt(c.getStartAt())
                .endAt(c.getEndAt())
                .viewCount((long) c.getViewCount())  // ✅ ép int sang long
                .build();
    }

    public Page<CampaignPublicDTO> getCampaignsPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PromotionalCampaign> campaigns = campaignRepo.findAll(pageable);
        return campaigns.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<CampaignPublicDTO> getTopActiveDiscountCampaigns() {
        // 🔸 Lấy thời điểm hiện tại
        var now = java.time.LocalDateTime.now();

        // 🔸 Lọc các chiến dịch còn hiệu lực
        List<PromotionalCampaign> activeCampaigns = campaignRepo.findAll().stream()
                .filter(c -> c.getStartAt() != null && c.getEndAt() != null)
                .filter(c -> !now.isBefore(c.getStartAt()) && !now.isAfter(c.getEndAt()))
                .sorted(Comparator.comparingInt(PromotionalCampaign::getViewCount).reversed())
                .limit(5)
                .toList();

        // 🔸 Map sang DTO
        return activeCampaigns.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


}
