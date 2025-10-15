/*package com.alotra.service;

import com.alotra.dto.ProductVariantDTO;
import com.alotra.entity.ProductVariant;
import com.alotra.entity.PromotionalCampaign;
import com.alotra.entity.PromotionTarget;
import com.alotra.repository.ProductVariantRepository;
import com.alotra.repository.PromotionalCampaignRepository;
import com.alotra.repository.PromotionTargetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final PromotionalCampaignRepository promotionalCampaignRepository;
    private final PromotionTargetRepository promotionTargetRepository;

    public ProductVariantService(ProductVariantRepository variantRepository,
                                 PromotionalCampaignRepository promotionalCampaignRepository,
                                 PromotionTargetRepository promotionTargetRepository) {
        this.variantRepository = variantRepository;
        this.promotionalCampaignRepository = promotionalCampaignRepository;
        this.promotionTargetRepository = promotionTargetRepository;
    }

    // 🟢 Trả về entity (nếu nội bộ service cần dùng)
    public List<ProductVariant> getActiveVariantsByProductId(Long productId) {
        return variantRepository.findByProduct_IdAndStatus(productId, "ACTIVE");
    }

    public ProductVariant getById(Long id) {
        return variantRepository.findById(id).orElse(null);
    }

    // 🟢 Trả về DTO có tính khuyến mãi
    public List<ProductVariantDTO> getVariantDTOsByProductId(Long productId) {
        List<ProductVariant> variants = getActiveVariantsByProductId(productId);
        List<PromotionalCampaign> campaigns = promotionalCampaignRepository.findActiveCampaigns(LocalDateTime.now());
        List<PromotionTarget> targets = promotionTargetRepository.findByProductId(productId);

        return variants.stream().map(v -> {
            BigDecimal original = v.getPrice();
            if (original == null) original = BigDecimal.ZERO;

            BigDecimal bestPrice = original;
            int discountPercent = 0;
            boolean hasDiscount = false;

            for (PromotionalCampaign campaign : campaigns) {
                boolean isTarget = targets.stream()
                        .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));
                if (!isTarget) continue;

                BigDecimal rawDiscounted;
                BigDecimal finalDiscounted;

                switch (campaign.getType()) {
                    case ORDER_PERCENT:
                        rawDiscounted = original.subtract(
                                original.multiply(campaign.getValue())
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        );
                        finalDiscounted = roundUpThousand(rawDiscounted);
                        break;
                    case ORDER_FIXED:
                        rawDiscounted = original.subtract(campaign.getValue()).max(BigDecimal.ZERO);
                        finalDiscounted = rawDiscounted;
                        break;
                    default:
                        continue;
                }

                if (rawDiscounted.compareTo(original) < 0 && finalDiscounted.compareTo(bestPrice) < 0) {
                    bestPrice = finalDiscounted;
                    hasDiscount = true;

                    if (campaign.getType() == PromotionalCampaign.CampaignType.ORDER_PERCENT) {
                        discountPercent = campaign.getValue().intValue();
                    } else {
                        if (original.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal percent = BigDecimal.valueOf(100)
                                    .subtract(bestPrice.multiply(BigDecimal.valueOf(100))
                                            .divide(original, 0, RoundingMode.HALF_UP));
                            discountPercent = percent.intValue();
                        } else {
                            discountPercent = 0;
                        }
                    }
                }
            }

            ProductVariantDTO dto = new ProductVariantDTO();
            dto.setId(v.getId());
            dto.setSizeId(v.getSize().getId());

            // ✅ Thêm sizeCode
            dto.setSizeCode(v.getSize().getCode());

            // ✅ Giữ lại sizeName nếu có nơi khác dùng
            dto.setSizeName(v.getSize().getName());

            dto.setOriginalPrice(original);
            dto.setDiscountedPrice(hasDiscount ? bestPrice : original);
            dto.setHasDiscount(hasDiscount);
            dto.setDiscountPercent(discountPercent);
            dto.setPrice(hasDiscount ? bestPrice : original);
            dto.setStatus(v.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    // ✅ Làm tròn lên 1.000đ
    private BigDecimal roundUpThousand(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        BigDecimal thousand = new BigDecimal("1000");
        if (price.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return price.divide(thousand, 0, RoundingMode.CEILING).multiply(thousand);
    }
}*/
package com.alotra.service;

import com.alotra.dto.ProductVariantDTO;
import com.alotra.entity.ProductVariant;
import com.alotra.entity.PromotionalCampaign;
import com.alotra.entity.PromotionTarget;
import com.alotra.repository.ProductVariantRepository;
import com.alotra.repository.PromotionalCampaignRepository;
import com.alotra.repository.PromotionTargetRepository;
import com.alotra.repository.CouponRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final PromotionalCampaignRepository promotionalCampaignRepository;
    private final PromotionTargetRepository promotionTargetRepository;
    private final CouponRepository couponRepository; // 🆕 Thêm repo để kiểm tra campaign có coupon không

    public ProductVariantService(ProductVariantRepository variantRepository,
                                 PromotionalCampaignRepository promotionalCampaignRepository,
                                 PromotionTargetRepository promotionTargetRepository,
                                 CouponRepository couponRepository) {
        this.variantRepository = variantRepository;
        this.promotionalCampaignRepository = promotionalCampaignRepository;
        this.promotionTargetRepository = promotionTargetRepository;
        this.couponRepository = couponRepository;
    }

    // 🟢 Trả về entity (nếu nội bộ service cần dùng)
    public List<ProductVariant> getActiveVariantsByProductId(Long productId) {
        return variantRepository.findByProduct_IdAndStatus(productId, "ACTIVE");
    }

    public ProductVariant getById(Long id) {
        return variantRepository.findById(id).orElse(null);
    }

    // 🟢 Trả về DTO có tính khuyến mãi (lọc bỏ campaign có coupon)
    public List<ProductVariantDTO> getVariantDTOsByProductId(Long productId) {
        List<ProductVariant> variants = getActiveVariantsByProductId(productId);

        // 🧠 Chỉ lấy campaign active KHÔNG có coupon
        List<PromotionalCampaign> campaigns = promotionalCampaignRepository
                .findActiveCampaigns(LocalDateTime.now())
                .stream()
                .filter(c -> !couponRepository.existsByCampaignId(c.getId()))
                .collect(Collectors.toList());

        List<PromotionTarget> targets = promotionTargetRepository.findByProductId(productId);

        return variants.stream().map(v -> {
            BigDecimal original = v.getPrice();
            if (original == null) original = BigDecimal.ZERO;

            BigDecimal bestPrice = original;
            int discountPercent = 0;
            boolean hasDiscount = false;

            for (PromotionalCampaign campaign : campaigns) {
                boolean isTarget = targets.stream()
                        .anyMatch(t -> t.getCampaign().getId().equals(campaign.getId()));
                if (!isTarget) continue;

                BigDecimal rawDiscounted;
                BigDecimal finalDiscounted;

                switch (campaign.getType()) {
                    case ORDER_PERCENT:
                        rawDiscounted = original.subtract(
                                original.multiply(campaign.getValue())
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        );
                        finalDiscounted = roundUpThousand(rawDiscounted);
                        break;

                    case ORDER_FIXED:
                        rawDiscounted = original.subtract(campaign.getValue()).max(BigDecimal.ZERO);
                        finalDiscounted = rawDiscounted;
                        break;

                    default:
                        continue;
                }

                if (rawDiscounted.compareTo(original) < 0 && finalDiscounted.compareTo(bestPrice) < 0) {
                    bestPrice = finalDiscounted;
                    hasDiscount = true;

                    if (campaign.getType() == PromotionalCampaign.CampaignType.ORDER_PERCENT) {
                        discountPercent = campaign.getValue().intValue();
                    } else {
                        if (original.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal percent = BigDecimal.valueOf(100)
                                    .subtract(bestPrice.multiply(BigDecimal.valueOf(100))
                                            .divide(original, 0, RoundingMode.HALF_UP));
                            discountPercent = percent.intValue();
                        } else {
                            discountPercent = 0;
                        }
                    }
                }
            }

            ProductVariantDTO dto = new ProductVariantDTO();
            dto.setId(v.getId());
            dto.setSizeId(v.getSize().getId());
            dto.setSizeCode(v.getSize().getCode());
            dto.setSizeName(v.getSize().getName());
            dto.setOriginalPrice(original);
            dto.setDiscountedPrice(hasDiscount ? bestPrice : original);
            dto.setHasDiscount(hasDiscount);
            dto.setDiscountPercent(discountPercent);
            dto.setPrice(hasDiscount ? bestPrice : original);
            dto.setStatus(v.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    // ✅ Làm tròn lên 1.000đ
    private BigDecimal roundUpThousand(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        BigDecimal thousand = new BigDecimal("1000");
        if (price.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return price.divide(thousand, 0, RoundingMode.CEILING).multiply(thousand);
    }
}

