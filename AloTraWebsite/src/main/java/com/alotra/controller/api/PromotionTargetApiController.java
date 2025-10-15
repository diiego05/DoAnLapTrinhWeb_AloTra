package com.alotra.controller.api;

import com.alotra.dto.CampaignTargetRequest;
import com.alotra.dto.PromotionTargetDTO;
import com.alotra.service.PromotionTargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/promotions/targets")
@RequiredArgsConstructor
public class PromotionTargetApiController {

    private final PromotionTargetService targetService;

    /**
     * 🟢 Lấy danh sách target theo ID chiến dịch (dùng DTO)
     * GET /api/admin/promotions/targets/by-campaign/{campaignId}
     */
    @GetMapping("/by-campaign/{campaignId}")
    public ResponseEntity<List<PromotionTargetDTO>> getByCampaign(@PathVariable Long campaignId) {
        return ResponseEntity.ok(targetService.getTargetDTOsByCampaign(campaignId));
    }

    /**
     * 🟢 Lấy danh sách target theo sản phẩm (dành cho mục đích khác)
     * GET /api/admin/promotions/targets/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<PromotionTargetDTO>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(targetService.getTargetDTOsByProduct(productId));
    }

    /**
     * 🟡 Gán đối tượng áp dụng cho chiến dịch
     * POST /api/admin/promotions/targets/{campaignId}
     */
    @PostMapping("/{campaignId}")
    public ResponseEntity<?> setTargets(
            @PathVariable Long campaignId,
            @RequestBody CampaignTargetRequest request
    ) {
        targetService.setTargetsForCampaign(campaignId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 🔴 Xóa tất cả target của chiến dịch
     * DELETE /api/admin/promotions/targets/campaign/{campaignId}
     */
    @DeleteMapping("/campaign/{campaignId}")
    public ResponseEntity<?> deleteByCampaign(@PathVariable Long campaignId) {
        targetService.deleteByCampaignId(campaignId);
        return ResponseEntity.ok().build();
    }

    /**
     * 🔴 Xóa 1 target cụ thể
     * DELETE /api/admin/promotions/targets/single/{id}
     */
    @DeleteMapping("/single/{id}")
    public ResponseEntity<?> deleteSingle(@PathVariable Long id) {
        targetService.delete(id);
        return ResponseEntity.ok().build();
    }
}
