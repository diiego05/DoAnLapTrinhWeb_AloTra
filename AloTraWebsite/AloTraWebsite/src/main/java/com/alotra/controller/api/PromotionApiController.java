package com.alotra.controller.api;

import com.alotra.entity.PromotionalCampaign;
import com.alotra.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/promotions/campaigns")
@RequiredArgsConstructor
public class PromotionApiController {

    private final PromotionService promotionService;

    @GetMapping
    public List<PromotionalCampaign> list() {
        return promotionService.getAll();
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> create(@RequestParam String name,
                                    @RequestParam(required = false) String description,
                                    @RequestParam String type,
                                    @RequestParam BigDecimal value,
                                    @RequestParam LocalDateTime startAt,
                                    @RequestParam LocalDateTime endAt,
                                    @RequestParam(required = false) MultipartFile bannerFile) {
        return ResponseEntity.ok(promotionService.create(name, description, type, value, startAt, endAt, bannerFile));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestParam String name,
                                    @RequestParam(required = false) String description,
                                    @RequestParam String type,
                                    @RequestParam BigDecimal value,
                                    @RequestParam LocalDateTime startAt,
                                    @RequestParam LocalDateTime endAt,
                                    @RequestParam(required = false) MultipartFile bannerFile) {
        return ResponseEntity.ok(promotionService.update(id, name, description, type, value, startAt, endAt, bannerFile));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        promotionService.delete(id);
    }
}
