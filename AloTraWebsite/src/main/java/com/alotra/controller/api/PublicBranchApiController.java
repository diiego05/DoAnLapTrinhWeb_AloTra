package com.alotra.controller.api;

import com.alotra.dto.BranchDTO;
import com.alotra.entity.Branch;
import com.alotra.service.BranchService;
import com.alotra.service.AddressService;
import com.alotra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/branches")
@RequiredArgsConstructor
public class PublicBranchApiController {

    private final BranchService branchService;
    private final AddressService addressService;
    private final UserService userService;

    /**
     * 📦 Lấy danh sách chi nhánh khả dụng cho các sản phẩm trong giỏ hàng.
     * - Input: List<Long> variantIds
     * - Output: Danh sách Branch đáp ứng đủ tất cả variant
     */
    @PostMapping("/available")
    public List<Branch> getAvailableBranches(@RequestBody List<Long> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) return List.of();
        return branchService.findAvailableBranches(variantIds);
    }


    /**
     * 🏪 Kiểm tra khả dụng của các CartItem tại một chi nhánh cụ thể.
     * - Input: branchId + List<Long> cartItemIds
     * - Output: Danh sách cartItemId KHÔNG khả dụng tại chi nhánh
     */
    @PostMapping("/{branchId}/check-availability")
    public List<Long> checkCartItemAvailability(
            @PathVariable Long branchId,
            @RequestBody List<Long> cartItemIds
    ) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return List.of();
        }
        return branchService.checkCartItemAvailability(branchId, cartItemIds);
    }

    @GetMapping("/active")
    public List<BranchDTO> getActiveBranches() {
        return branchService.getAllBranchesActiveDTO();
    }

    /**
     * 📍 Gợi ý chi nhánh ACTIVE gần nhất theo lat/lng hoặc addressId
     * ✅ Không yêu cầu đăng nhập - dùng cho checkout public
     */
    @GetMapping("/nearest")
    public ResponseEntity<BranchDTO> getNearestBranch(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Long addressId
    ) {
        System.out.println("🔍 [DEBUG] getNearestBranch called - addressId: " + addressId + ", lat: " + lat + ", lng: " + lng);
        
        Double qLat = lat, qLng = lng;

        // ✅ Nếu có addressId, lấy tọa độ từ address (không cần check user)
        if (qLat == null || qLng == null) {
            if (addressId == null) {
                System.out.println("❌ [DEBUG] Missing both coordinates and addressId");
                return ResponseEntity.badRequest().build();
            }

            // ✅ Thử lấy userId nếu user đã đăng nhập
            Long uid = null;
            try {
                uid = userService.getCurrentUserId();
                System.out.println("✅ [DEBUG] User logged in - userId: " + uid);
            } catch (Exception e) {
                System.out.println("⚠️ [DEBUG] User not logged in, proceeding without userId");
            }

            // ✅ Nếu không có userId, vẫn thử lấy tọa độ từ address trực tiếp
            var coordsOpt = addressService.getCoordinates(uid, addressId);
            if (coordsOpt.isEmpty()) {
                System.out.println("❌ [DEBUG] Cannot find coordinates for addressId: " + addressId);
                return ResponseEntity.notFound().build();
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
        } else {
            System.out.println("⚠️ [DEBUG] No active branch found near coordinates");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 🏪 Lấy danh sách chi nhánh có sản phẩm cụ thể
     * @param productId ID của sản phẩm
     * @return Danh sách chi nhánh ACTIVE có sản phẩm này
     */
    @GetMapping("/with-product/{productId}")
    public ResponseEntity<List<BranchDTO>> getBranchesWithProduct(@PathVariable Long productId) {
        List<BranchDTO> branches = branchService.findBranchesWithProduct(productId);
        return ResponseEntity.ok(branches);
    }

}