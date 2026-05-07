package com.alotra.controller.api;

import com.alotra.dto.CouponDTO;
import com.alotra.dto.CouponValidationResult;
import com.alotra.entity.Coupon;
import com.alotra.enums.CouponType;
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
public class CouponApiController{

    private final CouponService couponService;

    // ====================== 📢 PUBLIC API ======================

    /**
     * ✅ API xác thực mã giảm giá (kèm kiểm tra productId trong giỏ hàng).
     * FE gọi: POST /api/public/coupons/validate/{code}?subtotal=xxx&shippingFee=yyy
     * Body: [productId1, productId2, ...]
     */
    @PostMapping("/api/public/coupons/validate/{code}")
    public ResponseEntity<?> validate(
            @PathVariable String code,
            @RequestParam(required=false) BigDecimal subtotal,
            @RequestParam(required=false) BigDecimal orderTotal,
            @RequestParam(required=false) BigDecimal shippingFeeBase,
            @RequestBody(required=false) List<Long> productIds
    ){
        BigDecimal sub=(subtotal!=null)? subtotal: orderTotal;
        if(sub==null){
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "message","Thiếu tham số subtotal (hoặc orderTotal)."
            ));
        }
        if(productIds==null) productIds=java.util.List.of();

        Coupon coupon=couponService.validateCoupon(code,sub,productIds);

        String rawType=coupon.getType()==null? "": coupon.getType().trim();
        String up=rawType.toUpperCase();

        // Chuẩn hoá type cho đúng DTO (PERCENT | AMOUNT)
        String type;
        if(up.contains("PERCENT")) type="PERCENT";
        else if(up.contains("AMOUNT")||up.contains("FIXED")) type="AMOUNT";
        else type=rawType;

        java.util.List<String> appliesTo;
        if(up.contains("BOTH")){
            appliesTo=java.util.List.of("ORDER","SHIPPING");
        }else if(up.contains("SHIPPING")){
            appliesTo=java.util.List.of("SHIPPING");
        }else{
            appliesTo=java.util.List.of("ORDER");
        }

        Integer percent=null;
        BigDecimal amount=null;
        if("PERCENT".equalsIgnoreCase(type)){
            percent=coupon.getValue()!=null? coupon.getValue().intValue():0;
        }else if("AMOUNT".equalsIgnoreCase(type)){
            amount=coupon.getValue();
        }

        BigDecimal itemsDiscount=BigDecimal.ZERO;
        BigDecimal shippingDiscount=null;

        // ORDER
        if(appliesTo.contains("ORDER")){
            itemsDiscount=couponService.calculateDiscount(coupon,sub,BigDecimal.ZERO);
        }

        // SHIPPING: nếu FE không gửi shippingFeeBase thì trả null để FE biết "chưa tính"
        if(appliesTo.contains("SHIPPING")){
            if(shippingFeeBase==null){
                shippingDiscount=null;
            }else{
                shippingDiscount=couponService.calculateDiscount(coupon,BigDecimal.ZERO,shippingFeeBase);
            }
        }

        CouponValidationResult body=CouponValidationResult.builder()
                .type(type)
                .percent(percent)
                .amount(amount)
                .maxDiscount(coupon.getMaxDiscount())
                .appliesTo(appliesTo)
                .computed(new CouponValidationResult.Computed(itemsDiscount,shippingDiscount))
                .build();

        return ResponseEntity.ok(body);
    }



    // ====================== 🛡️ ADMIN API ======================

    @GetMapping("/api/admin/promotions/coupons")
    public List<CouponDTO> getAll(){
        return couponService.getAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/api/admin/promotions/coupons")
    public CouponDTO create(@RequestParam String code,
                            @RequestParam String type,
                            @RequestParam BigDecimal value,
                            @RequestParam(required=false) BigDecimal maxDiscount,
                            @RequestParam(required=false) BigDecimal minOrderTotal,
                            @RequestParam(required=false) Long campaignId,
                            @RequestParam(required=false) Integer usageLimit){

        Coupon c=couponService.create(code,type,value,maxDiscount,minOrderTotal,campaignId,usageLimit);
        return toDTO(c);
    }

    @PutMapping("/api/admin/promotions/coupons/{id}")
    public CouponDTO update(@PathVariable Long id,
                            @RequestParam String code,
                            @RequestParam String type,
                            @RequestParam BigDecimal value,
                            @RequestParam(required=false) BigDecimal maxDiscount,
                            @RequestParam(required=false) BigDecimal minOrderTotal,
                            @RequestParam(required=false) Long campaignId,
                            @RequestParam(required=false) Integer usageLimit,
                            @RequestParam(required=false)
                            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                            @RequestParam(required=false)
                            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt){

        Coupon c=couponService.update(
                id,code,type,value,
                maxDiscount,minOrderTotal,
                campaignId,usageLimit,startAt,endAt
        );
        return toDTO(c);
    }

    @DeleteMapping("/api/admin/promotions/coupons/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        couponService.delete(id);
        return ResponseEntity.ok().build();
    }

    private CouponDTO toDTO(Coupon c){
        return new CouponDTO(
                c.getId(),
                c.getCode(),
                c.getType(),
                c.getValue(),
                c.getMaxDiscount(),
                c.getMinOrderTotal(),
                c.getUsageLimit(),
                c.getUsedCount(),
                c.getCampaign()!=null?c.getCampaign().getName():null,
                c.getStartAt(),
                c.getEndAt()
        );
    }
}
