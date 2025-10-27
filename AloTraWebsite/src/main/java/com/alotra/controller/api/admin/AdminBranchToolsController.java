package com.alotra.controller.api.admin;

import com.alotra.service.BranchGeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
public class AdminBranchToolsController {

    private final BranchGeocodingService branchGeocodingService;

    // 📍 Geocode một chi nhánh theo ID
    @PostMapping("/{id}/geocode")
    public ResponseEntity<?> geocodeOne(@PathVariable Long id) {
        boolean ok = branchGeocodingService.geocodeBranch(id);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // 🔁 Geocode tất cả chi nhánh đang thiếu toạ độ
    @PostMapping("/geocode-missing")
    public ResponseEntity<?> geocodeMissing() {
        int updated = branchGeocodingService.fixMissingCoordinates();
        return ResponseEntity.ok(updated);
    }
}
