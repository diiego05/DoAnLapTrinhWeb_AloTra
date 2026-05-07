package com.alotra.controller.api;

import com.alotra.entity.Branch;
import com.alotra.repository.BranchRepository;
import com.alotra.service.GeocodingService;
import com.alotra.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug/geocoding")
@RequiredArgsConstructor
public class DebugGeocodingController {

    private final GeocodingService geocodingService;
    private final BranchRepository branchRepository;

    /**
     * 🔍 Debug: Kiểm tra tọa độ của một địa chỉ
     */
    @GetMapping("/check-address")
    public ResponseEntity<Map<String, Object>> checkAddress(@RequestParam String address) {
        Map<String, Object> result = new HashMap<>();
        result.put("input", address);

        var coords = geocodingService.geocodeAddress(address);
        if (coords.isPresent()) {
            var ll = coords.get();
            result.put("latitude", ll.latitude());
            result.put("longitude", ll.longitude());
            result.put("isValid", ll.latitude() >= 8.0 && ll.latitude() <= 24.5
                                 && ll.longitude() >= 102.0 && ll.longitude() <= 110.5);
            result.put("status", "success");
        } else {
            result.put("status", "failed");
            result.put("message", "Không thể geocode địa chỉ này");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 🔍 Debug: Liệt kê tất cả chi nhánh và tọa độ của chúng
     */
    @GetMapping("/list-branches")
    public ResponseEntity<List<Map<String, Object>>> listBranches() {
        List<Branch> branches = branchRepository.findByStatus("ACTIVE");

        List<Map<String, Object>> result = branches.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("name", b.getName());
            map.put("address", b.getAddress());
            map.put("latitude", b.getLatitude());
            map.put("longitude", b.getLongitude());
            map.put("hasCoordinates", b.getLatitude() != null && b.getLongitude() != null);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 🔍 Debug: Tính khoảng cách từ một địa chỉ đến tất cả chi nhánh
     */
    @GetMapping("/calculate-distances")
    public ResponseEntity<Map<String, Object>> calculateDistances(@RequestParam String address) {
        Map<String, Object> result = new HashMap<>();
        result.put("userAddress", address);

        // Geocode địa chỉ khách hàng
        var userCoords = geocodingService.geocodeAddress(address);
        if (userCoords.isEmpty()) {
            result.put("status", "error");
            result.put("message", "Không thể geocode địa chỉ khách hàng");
            return ResponseEntity.ok(result);
        }

        var userLL = userCoords.get();
        result.put("userLatitude", userLL.latitude());
        result.put("userLongitude", userLL.longitude());

        // Lấy tất cả chi nhánh ACTIVE có tọa độ
        List<Branch> branches = branchRepository.findByStatusAndLatitudeIsNotNullAndLongitudeIsNotNull("ACTIVE");

        List<Map<String, Object>> distances = branches.stream()
            .map(b -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", b.getId());
                map.put("name", b.getName());
                map.put("address", b.getAddress());
                map.put("latitude", b.getLatitude());
                map.put("longitude", b.getLongitude());

                double distance = LocationService.haversineKm(
                    userLL.latitude(), userLL.longitude(),
                    b.getLatitude(), b.getLongitude()
                );
                map.put("distanceKm", Math.round(distance * 100.0) / 100.0);

                return map;
            })
            .sorted((a, b) -> Double.compare(
                (Double) a.get("distanceKm"),
                (Double) b.get("distanceKm")
            ))
            .collect(Collectors.toList());

        result.put("branches", distances);
        result.put("nearestBranch", distances.isEmpty() ? null : distances.get(0));
        result.put("status", "success");

        return ResponseEntity.ok(result);
    }
}