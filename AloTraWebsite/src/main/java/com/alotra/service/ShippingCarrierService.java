package com.alotra.service;

import com.alotra.entity.ShippingCarrier;
import com.alotra.dto.ShippingFeeDTO;
import com.alotra.entity.PromotionalCampaign;
import com.alotra.repository.ShippingCarrierRepository;
import com.alotra.repository.PromotionalCampaignRepository;
import com.alotra.repository.CouponRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShippingCarrierService {

    private final ShippingCarrierRepository repo;
    private final PromotionalCampaignRepository campaignRepo;
    private final CouponRepository couponRepo;

    public ShippingCarrierService(
            ShippingCarrierRepository repo,
            PromotionalCampaignRepository campaignRepo,
            CouponRepository couponRepo
    ) {
        this.repo = repo;
        this.campaignRepo = campaignRepo;
        this.couponRepo = couponRepo;
    }

    public Optional<ShippingCarrier> findById(Long id) {
        return repo.findById(id);
    }

    public List<ShippingCarrier> getAll() {
        return repo.findAllByOrderByIdDesc();
    }

    public ShippingCarrier create(ShippingCarrier carrier) {
        return repo.save(carrier);
    }

    public ShippingCarrier update(Long id, ShippingCarrier updated) {
        ShippingCarrier c = repo.findById(id).orElseThrow();
        c.setName(updated.getName());
        c.setLogoUrl(updated.getLogoUrl());
        c.setBaseFee(updated.getBaseFee());
        c.setActive(updated.isActive());
        return repo.save(c);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public void toggleStatus(Long id) {
        ShippingCarrier c = repo.findById(id).orElseThrow();
        c.setActive(!c.isActive());
        repo.save(c);
    }

    public List<ShippingCarrier> getActiveCarriers() {
        return repo.findByIsActiveTrue();
    }

    public ShippingCarrier findActiveById(Long id) {
        return repo.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Nhà vận chuyển không tồn tại hoặc không hoạt động"));
    }

    // ================= 🧮 TÍNH PHÍ VẬN CHUYỂN (ÁP DỤNG KHUYẾN MÃI KHÔNG COUPON) =================
    public BigDecimal getFee(Long carrierId) {
        ShippingCarrier carrier = findActiveById(carrierId);
        BigDecimal baseFee = carrier.getBaseFee();
        if (baseFee == null) baseFee = BigDecimal.ZERO;

        // 🧭 Lấy các campaign active KHÔNG có coupon
        List<PromotionalCampaign> campaigns = campaignRepo
                .findActiveCampaigns(LocalDateTime.now())
                .stream()
                .filter(c -> !couponRepo.existsByCampaignId(c.getId()))
                .collect(Collectors.toList());

        BigDecimal bestFee = baseFee;

        for (PromotionalCampaign campaign : campaigns) {
            BigDecimal discounted = baseFee;

            switch (campaign.getType()) {
                case ORDER_PERCENT:
                    discounted = baseFee.subtract(
                            baseFee.multiply(campaign.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    );
                    discounted = roundUpThousand(discounted);
                    break;
                case ORDER_FIXED:
                    discounted = baseFee.subtract(campaign.getValue()).max(BigDecimal.ZERO);
                    break;
                default:
                    continue;
            }

            if (discounted.compareTo(bestFee) < 0) {
                bestFee = discounted;
            }
        }

        return bestFee;
    }

    // ✅ Làm tròn lên 1.000đ giống Product
    private BigDecimal roundUpThousand(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        BigDecimal thousand = new BigDecimal("1000");
        if (price.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return price.divide(thousand, 0, RoundingMode.CEILING).multiply(thousand);
    }

    public ShippingFeeDTO getFeeDetail(Long id) {
        ShippingCarrier carrier = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà vận chuyển"));

        BigDecimal baseFee = carrier.getBaseFee() != null ? carrier.getBaseFee() : BigDecimal.ZERO;
        BigDecimal bestFee = baseFee;
        PromotionalCampaign bestCampaign = null;

        List<PromotionalCampaign> campaigns = campaignRepo.findActiveCampaigns(LocalDateTime.now())
                .stream()
                .filter(c -> !couponRepo.existsByCampaignId(c.getId()))
                .filter(c -> c.getType() == PromotionalCampaign.CampaignType.SHIPPING_PERCENT
                          || c.getType() == PromotionalCampaign.CampaignType.SHIPPING_FIXED)
                .collect(Collectors.toList());

        for (PromotionalCampaign campaign : campaigns) {
            BigDecimal finalDiscounted;

            switch (campaign.getType()) {
                case SHIPPING_PERCENT:
                    BigDecimal percentVal = campaign.getValue();
                    if (percentVal.compareTo(BigDecimal.valueOf(100)) > 0) {
                        // nếu lỡ lưu là tiền → quy ra %
                        percentVal = percentVal.multiply(BigDecimal.valueOf(100))
                                               .divide(baseFee, 2, RoundingMode.HALF_UP);
                    }
                    BigDecimal rawPercent = baseFee.subtract(
                            baseFee.multiply(percentVal)
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    );
                    finalDiscounted = roundUpThousand(rawPercent);
                    break;

                case SHIPPING_FIXED:
                    finalDiscounted = baseFee.subtract(campaign.getValue()).max(BigDecimal.ZERO);
                    break;

                default:
                    continue;
            }

            // chỉ chọn campaign tốt nhất (rẻ nhất)
            if (finalDiscounted.compareTo(bestFee) < 0) {
                bestFee = finalDiscounted;
                bestCampaign = campaign;
            }
        }

        boolean hasDiscount = bestCampaign != null;
        int discountPercent = 0;

        if (hasDiscount) {
            if (bestCampaign.getType() == PromotionalCampaign.CampaignType.SHIPPING_PERCENT) {
                BigDecimal val = bestCampaign.getValue();
                if (val.compareTo(BigDecimal.valueOf(100)) > 0) {
                    BigDecimal percent = val.multiply(BigDecimal.valueOf(100))
                            .divide(baseFee, 0, RoundingMode.HALF_UP);
                    discountPercent = percent.intValue();
                } else {
                    discountPercent = val.intValue();
                }
            } else if (baseFee.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percent = BigDecimal.valueOf(100)
                        .subtract(bestFee.multiply(BigDecimal.valueOf(100))
                                .divide(baseFee, 0, RoundingMode.HALF_UP));
                discountPercent = percent.intValue();
            }
        }

        return ShippingFeeDTO.builder()
                .id(carrier.getId())
                .name(carrier.getName())
                .baseFee(baseFee)
                .discountedFee(hasDiscount ? bestFee : baseFee)
                .hasDiscount(hasDiscount)
                .discountPercent(discountPercent)
                .build();
    }





}
