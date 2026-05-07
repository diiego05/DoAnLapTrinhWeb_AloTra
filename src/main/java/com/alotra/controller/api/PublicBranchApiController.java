package com.alotra.controller.api;

import com.alotra.dto.BranchDTO;
import com.alotra.entity.Branch;
import com.alotra.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.alotra.service.AddressService;
import com.alotra.service.UserService;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/branches")
@RequiredArgsConstructor
public class PublicBranchApiController {

    private final BranchService branchService;
    private final AddressService addressService;
    private final UserService userService;

    @PostMapping("/available")
    public List<Branch> getAvailableBranches(@RequestBody List<Long> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) return List.of();
        return branchService.findAvailableBranches(variantIds);
    }

    @PostMapping("/{branchId}/check-availability")
    public List<Long> checkCartItemAvailability(
            @PathVariable Long branchId,
            @RequestBody List<Long> cartItemIds
    ) {
        if (cartItemIds == null || cartItemIds.isEmpty()) return List.of();
        return branchService.checkCartItemAvailability(branchId, cartItemIds);
    }

    @GetMapping("/active")
    public List<BranchDTO> getActiveBranches() {
        return branchService.getAllBranchesActiveDTO();
    }

    @GetMapping("/nearest")
    public ResponseEntity<?> getNearestBranch(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Long addressId
    ) {
        System.out.println("🔍 [DEBUG] getNearestBranch called - addressId: " + addressId + ", lat: " + lat + ", lng: " + lng);

        Double qLat = lat, qLng = lng;

        if (qLat == null || qLng == null) {
            if (addressId == null) {
                System.out.println("❌ [DEBUG] Missing both coordinates and addressId");
                return ResponseEntity.badRequest().body(Map.of("message", "Thiếu lat/lng hoặc addressId"));
            }

            Long uid = null;
            try {
                uid = userService.getCurrentUserId();
                System.out.println("✅ [DEBUG] User logged in - userId: " + uid);
            } catch (Exception e) {
                System.out.println("⚠️ [DEBUG] User not logged in, proceeding without userId");
            }

            var coordsOpt = addressService.getCoordinates(uid, addressId);
            if (coordsOpt.isEmpty()) {
                System.out.println("❌ [DEBUG] Cannot find coordinates for addressId: " + addressId);
                return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy toạ độ cho địa chỉ này"));
            }

            var coords = coordsOpt.get();
            qLat = coords.latitude();
            qLng = coords.longitude();
            System.out.println("✅ [DEBUG] Got coordinates - lat: " + qLat + ", lng: " + qLng);
        }

        var nearest = branchService.findNearestActiveBranch(qLat, qLng);

        if (nearest != null) {
            System.out.println("✅ [DEBUG] Found nearest branch: " + nearest.getName());
            return ResponseEntity.ok(nearest);
        }

        // ✅ Trường hợp: không có branch (hoặc branch gần nhất > 20km vì service đã return null)
        System.out.println("⚠️ [DEBUG] No active branch found within range");
        return ResponseEntity.status(404).body(
                Map.of("message", "Hiện chưa có chi nhánh trong bán kính 20km từ địa chỉ này. Rất tiếc về sự bất tiện này.\r\n")
        );
    }

    @GetMapping("/with-product/{productId}")
    public ResponseEntity<List<BranchDTO>> getBranchesWithProduct(@PathVariable Long productId) {
        List<BranchDTO> branches = branchService.findBranchesWithProduct(productId);
        return ResponseEntity.ok(branches);
    }
}
