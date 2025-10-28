package com.alotra.controller.api;


import com.alotra.dto.BranchDistanceDTO;
import com.alotra.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationApiController {

    private final LocationService locationService;

    // 📍 1) Tìm chi nhánh gần nhất theo toạ độ
    @GetMapping("/nearest")
    public ResponseEntity<List<BranchDistanceDTO>> nearest(
            @RequestParam("lat") double latitude,
            @RequestParam("lng") double longitude,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ResponseEntity.ok(locationService.findNearestBranches(latitude, longitude, limit, status));
    }

    // 🏠 2) Tìm chi nhánh gần nhất theo địa chỉ văn bản tự do
    @GetMapping("/nearest-by-address")
    public ResponseEntity<List<BranchDistanceDTO>> nearestByAddress(
            @RequestParam("address") String address,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ResponseEntity.ok(locationService.findNearestByAddress(address, limit, status));
    }

    // 👤 3) Tìm chi nhánh gần nhất cho user hiện tại (dựa vào địa chỉ mặc định)
    @GetMapping("/nearest-for-me")
    public ResponseEntity<List<BranchDistanceDTO>> nearestForCurrentUser(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ResponseEntity.ok(locationService.findNearestForCurrentUser(limit, status));
    }
}