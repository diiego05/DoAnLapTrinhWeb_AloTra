package com.alotra.controller.api;

import com.alotra.dto.CampaignPublicDTO;
import com.alotra.service.CampaignPublicService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/promotions")
@RequiredArgsConstructor
public class CampaignPublicApiController {

    private final CampaignPublicService campaignPublicService;

    /**
     * 🟢 Lấy danh sách top chiến dịch khuyến mãi (theo view)
     * GET /api/public/promotions/top
     */
    @GetMapping("/top")
    public ResponseEntity<List<CampaignPublicDTO>> getTopCampaigns() {
        return ResponseEntity.ok(campaignPublicService.getTopCampaigns());
    }

    /**
     * 🟢 Lấy chi tiết 1 chiến dịch và tự động tăng lượt view
     * GET /api/public/promotions/{campaignId}
     */
    @GetMapping("/{campaignId}")
    public ResponseEntity<CampaignPublicDTO> getCampaignDetail(@PathVariable Long campaignId) {
        return ResponseEntity.ok(
                campaignPublicService.getCampaignDetailAndIncreaseView(campaignId)
        );
    }

    @GetMapping
    public ResponseEntity<Page<CampaignPublicDTO>> getCampaignsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        return ResponseEntity.ok(campaignPublicService.getCampaignsPaged(page, size));
    }

}
