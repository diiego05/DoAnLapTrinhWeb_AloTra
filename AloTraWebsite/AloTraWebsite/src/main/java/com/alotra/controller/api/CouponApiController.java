package com.alotra.controller.api;

import com.alotra.dto.CouponDTO;
import com.alotra.entity.Coupon;
import com.alotra.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CouponApiController {

    private final CouponService couponService;

    // ====================== 📢 PUBLIC API ======================
    @GetMapping("/api/public/coupons/validate/{code}")
    public ResponseEntity<?> validate(@PathVariable String code,
                                      @RequestParam BigDecimal orderTotal) {
        Coupon coupon = couponService.validateCoupon(code, orderTotal);
        BigDecimal discount = couponService.calculateDiscount(coupon, orderTotal);
        return ResponseEntity.ok(discount);
    }

    // ====================== 🛡️ ADMIN API ======================
    @GetMapping("/api/admin/promotions/coupons")
    public List<CouponDTO> getAll() {
        return couponService.getAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/api/admin/promotions/coupons")
    public CouponDTO create(@RequestParam String code,
                            @RequestParam String type,
                            @RequestParam BigDecimal value,
                            @RequestParam(required = false) BigDecimal maxDiscount,
                            @RequestParam(required = false) BigDecimal minOrderTotal,
                            @RequestParam(required = false) Long campaignId,
                            @RequestParam(required = false) Integer usageLimit) {

        Coupon c = couponService.create(code, type, value, maxDiscount, minOrderTotal, campaignId, usageLimit);
        return toDTO(c);
    }

    // ✅ Cập nhật coupon
    @PutMapping("/api/admin/promotions/coupons/{id}")
    public CouponDTO update(@PathVariable Long id,
                            @RequestParam String code,
                            @RequestParam String type,
                            @RequestParam BigDecimal value,
                            @RequestParam(required = false) BigDecimal maxDiscount,
                            @RequestParam(required = false) BigDecimal minOrderTotal,
                            @RequestParam(required = false) Long campaignId,
                            @RequestParam(required = false) Integer usageLimit,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt) {

        Coupon c = couponService.update(id, code, type, value, maxDiscount, minOrderTotal, campaignId, usageLimit, startAt, endAt);
        return toDTO(c);
    }

    @DeleteMapping("/api/admin/promotions/coupons/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.ok().build();
    }

    // ====================== 🧭 Entity -> DTO ======================
    private CouponDTO toDTO(Coupon c) {
        return new CouponDTO(
                c.getId(),
                c.getCode(),
                c.getType(),
                c.getValue(),
                c.getMaxDiscount(),
                c.getMinOrderTotal(),
                c.getUsageLimit(),
                c.getUsedCount(),
                c.getCampaign() != null ? c.getCampaign().getName() : null,
                c.getStartAt(),
                c.getEndAt()
        );
    }
}
