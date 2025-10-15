package com.alotra.controller.api;

import com.alotra.dto.BranchDTO;
import com.alotra.entity.Branch;
import com.alotra.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/branches")
@RequiredArgsConstructor
public class PublicBranchApiController {

    private final BranchService branchService;

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

}
