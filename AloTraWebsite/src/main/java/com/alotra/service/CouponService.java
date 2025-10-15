package com.alotra.service;

import com.alotra.entity.Coupon;
import com.alotra.entity.PromotionalCampaign;
import com.alotra.entity.PromotionTarget;
import com.alotra.repository.CouponRepository;
import com.alotra.repository.PromotionalCampaignRepository;

import jakarta.transaction.Transactional;

import com.alotra.repository.PromotionTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final PromotionalCampaignRepository campaignRepository;
    private final PromotionTargetRepository promotionTargetRepository;

    // 📥 Lấy tất cả coupon
    public List<Coupon> getAll() {
        return couponRepository.findAll();
    }

    // ✅ Kiểm tra hợp lệ ngày hiệu lực
    private void validateDateRange(Coupon coupon) {
        if (coupon.getStartAt() != null && coupon.getEndAt() != null &&
                coupon.getStartAt().isAfter(coupon.getEndAt())) {
            throw new RuntimeException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }
    }

    // ➕ Tạo coupon mới
    public Coupon create(String code, String type, BigDecimal value,
                         BigDecimal maxDiscount, BigDecimal minOrderTotal,
                         Long campaignId, Integer usageLimit) {

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setType(type);
        coupon.setValue(value);
        coupon.setMaxDiscount(maxDiscount);
        coupon.setMinOrderTotal(minOrderTotal);
        coupon.setUsageLimit(usageLimit);
        coupon.setUsedCount(0);
        coupon.setStatus("ACTIVE");
        coupon.setCreatedAt(LocalDateTime.now());

        // Gắn chiến dịch nếu có
        if (campaignId != null) {
            PromotionalCampaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new RuntimeException("Chiến dịch không tồn tại"));
            coupon.setCampaign(campaign);
            coupon.setStartAt(campaign.getStartAt());
            coupon.setEndAt(campaign.getEndAt());
        }

        validateDateRange(coupon);
        return couponRepository.save(coupon);
    }

    // 🛠️ Cập nhật coupon
    public Coupon update(Long id, String code, String type, BigDecimal value,
                         BigDecimal maxDiscount, BigDecimal minOrderTotal,
                         Long campaignId, Integer usageLimit,
                         LocalDateTime startAt, LocalDateTime endAt) {

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

        coupon.setCode(code);
        coupon.setType(type);
        coupon.setValue(value);
        coupon.setMaxDiscount(maxDiscount);
        coupon.setMinOrderTotal(minOrderTotal);
        coupon.setUsageLimit(usageLimit);

        if (campaignId != null) {
            PromotionalCampaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new RuntimeException("Chiến dịch không tồn tại"));
            coupon.setCampaign(campaign);
            coupon.setStartAt(campaign.getStartAt());
            coupon.setEndAt(campaign.getEndAt());
        } else {
            coupon.setCampaign(null);
            coupon.setStartAt(startAt);
            coupon.setEndAt(endAt);
        }

        validateDateRange(coupon);
        return couponRepository.save(coupon);
    }

    // 🗑️ Xóa coupon
    public void delete(Long id) {
        couponRepository.deleteById(id);
    }

    // 🧾 Xác thực coupon (dành cho khách hàng nhập vào)
    // ✅ Đã bổ sung kiểm tra target trong campaign
    public Coupon validateCoupon(String code, BigDecimal orderTotal, List<Long> productIds) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStartAt() != null && coupon.getStartAt().isAfter(now))
            throw new RuntimeException("Mã giảm giá chưa có hiệu lực");

        if (coupon.getEndAt() != null && coupon.getEndAt().isBefore(now))
            throw new RuntimeException("Mã giảm giá đã hết hạn");

        if (coupon.getUsageLimit() != null &&
                coupon.getUsedCount() >= coupon.getUsageLimit())
            throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng");

        if (coupon.getMinOrderTotal() != null &&
                orderTotal.compareTo(coupon.getMinOrderTotal()) < 0)
            throw new RuntimeException("Đơn hàng chưa đạt mức tối thiểu để áp dụng mã");

        // 📌 Nếu coupon có campaign → kiểm tra productId có nằm trong target không
        if (coupon.getCampaign() != null && productIds != null && !productIds.isEmpty()) {
            List<PromotionTarget> targets = promotionTargetRepository.findByCampaignId(coupon.getCampaign().getId());
            if (!targets.isEmpty()) {
                // 🔸 Kiểm tra productIds có trùng với ProductId trong target không
                boolean match = productIds.stream().anyMatch(
                        pId -> targets.stream().anyMatch(t -> t.getProductId().equals(pId))
                );
                if (!match) {
                    throw new RuntimeException("Mã giảm giá không áp dụng cho sản phẩm trong giỏ hàng");
                }
            }
        }

        return coupon;
    }

    // 🧮 Tính số tiền được giảm
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderTotal) {
        if (coupon == null) return BigDecimal.ZERO;

        BigDecimal discount;
        if ("PERCENT".equalsIgnoreCase(coupon.getType())) {
            discount = orderTotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100));
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getValue();
        }

        // Không bao giờ vượt quá tổng đơn hàng
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }

        return discount;
    }
    @Transactional
    public void increaseUsedCount(Long couponId) {
        if (couponId == null) return;
        couponRepository.findById(couponId).ifPresent(coupon -> {
            int current = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
            coupon.setUsedCount(current + 1);
            couponRepository.save(coupon);
        });
    }

}
