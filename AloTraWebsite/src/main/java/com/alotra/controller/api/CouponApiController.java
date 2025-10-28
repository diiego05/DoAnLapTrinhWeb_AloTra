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

    /**
     * ✅ API xác thực mã giảm giá (kèm kiểm tra productId trong giỏ hàng).
     * FE gọi: POST /api/public/coupons/validate/{code}?orderTotal=xxx
     * Body: [productId1, productId2, ...]
     */
    @PostMapping("/api/public/coupons/validate/{code}")
    public ResponseEntity<?> validate(
            @PathVariable String code,
            @RequestParam BigDecimal orderTotal,
            @RequestBody List<Long> productIds
    ) {
        var coupon = couponService.validateCoupon(code, orderTotal, productIds);

        // --- Normalize type & appliesTo từ coupon.getType() ---
        // Ví dụ có thể là: "PERCENT", "AMOUNT", "SHIPPING_PERCENT", "ORDER_AMOUNT", "BOTH_PERCENT", ...
        String rawType = coupon.getType() == null ? "" : coupon.getType().trim();
        String up = rawType.toUpperCase();

        // Chuẩn hoá type về "PERCENT" | "AMOUNT"
        String type;
        if (up.contains("PERCENT")) type = "PERCENT";
        else if (up.contains("AMOUNT")) type = "AMOUNT";
        else type = rawType; // fallback (giữ nguyên nếu đã chuẩn)

        // Suy ra phạm vi áp dụng
        java.util.List<String> appliesTo;
        if (up.contains("BOTH")) {
            appliesTo = java.util.List.of("ORDER", "SHIPPING");
        } else if (up.contains("SHIPPING")) {
            appliesTo = java.util.List.of("SHIPPING");
        } else if (up.contains("ORDER")) {
            appliesTo = java.util.List.of("ORDER");
        } else {
            // Chưa có field riêng trong entity -> mặc định ORDER để tương thích
            appliesTo = java.util.List.of("ORDER");
        }

        // percent / amount theo type đã chuẩn hoá
        Integer percent = null;
        BigDecimal amount = null;
        if ("PERCENT".equalsIgnoreCase(type)) {
            percent = coupon.getValue() != null ? coupon.getValue().intValue() : 0;
        } else if ("AMOUNT".equalsIgnoreCase(type)) {
            amount = coupon.getValue();
        }

        // Tính sẵn giảm trên HÀNG nếu coupon áp dụng cho ORDER
        BigDecimal itemsDiscount = appliesTo.contains("ORDER")
                ? couponService.calculateDiscount(coupon, orderTotal)
                : BigDecimal.ZERO;

        var body = com.alotra.dto.CouponValidationResult.builder()
                .type(type)                                   // "PERCENT" | "AMOUNT"
                .percent(percent)                             // nếu type=PERCENT
                .amount(amount)                               // nếu type=AMOUNT
                .maxDiscount(coupon.getMaxDiscount())
                .appliesTo(appliesTo)                         // ["ORDER"] | ["SHIPPING"] | ["ORDER","SHIPPING"]
                .computed(new com.alotra.dto.CouponValidationResult.Computed(
                        itemsDiscount,                        // items: đã tính sẵn nếu áp ORDER
                        null                                  // shipping: để FE tự tính theo shippingFeeBase
                ))
                .build();

        return ResponseEntity.ok(body);
    }


    // ====================== 🛡️ ADMIN API ======================

    /**
     * 📜 Lấy tất cả coupon
     */
    @GetMapping("/api/admin/promotions/coupons")
    public List<CouponDTO> getAll() {
        return couponService.getAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ➕ Tạo coupon mới
     */
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

    /**
     * ✏️ Cập nhật coupon
     */
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

        Coupon c = couponService.update(
                id, code, type, value,
                maxDiscount, minOrderTotal,
                campaignId, usageLimit, startAt, endAt
        );
        return toDTO(c);
    }

    /**
     * 🗑️ Xóa coupon
     */
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
