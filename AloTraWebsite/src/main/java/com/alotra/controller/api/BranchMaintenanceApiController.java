package com.alotra.controller.api;

import com.alotra.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
public class BranchMaintenanceApiController {

    private final BranchService branchService;

    /**
     * 🧹 Backfill toạ độ cho tất cả chi nhánh chưa có hoặc không hợp lệ.
     * Trả về số lượng chi nhánh đã được cập nhật.
     */
    @PostMapping("/backfill-coordinates")
    public ResponseEntity<Map<String, Object>> backfillCoordinates() {
        int updated = branchService.backfillCoordinatesForAllBranches();
        return ResponseEntity.ok(Map.of(
                "updated", updated
        ));
    }
}
