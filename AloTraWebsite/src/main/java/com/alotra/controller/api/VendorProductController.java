package com.alotra.controller.api;

import com.alotra.dto.ProductDetailDTO;
import com.alotra.dto.VendorProductSummaryDTO;
import com.alotra.service.ProductService;
import com.alotra.service.UserService;
import com.alotra.service.VendorProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/products")
@RequiredArgsConstructor
public class VendorProductController {

    private final VendorProductService vendorProductService;
    private final ProductService productService;
    private final UserService userService;

    /**
     * 📌 Lấy branchId dựa vào vendorId (user hiện tại)
     */
    private Long getBranchIdForCurrentVendor() {
        Long vendorId = userService.getCurrentUserId();
        return vendorProductService.getBranchIdByManagerId(vendorId);
    }

    /**
     * 🧾 1️⃣ Lấy danh sách sản phẩm theo chi nhánh vendor hiện tại
     */
    @GetMapping
    public ResponseEntity<List<VendorProductSummaryDTO>> getProductsForBranch() {
        Long branchId = getBranchIdForCurrentVendor();
        List<VendorProductSummaryDTO> products = vendorProductService.getProductsForBranch(branchId);
        return ResponseEntity.ok(products);
    }

    /**
     * 🔍 2️⃣ Lấy chi tiết sản phẩm theo id (vendor chỉ xem được sản phẩm thuộc chi nhánh của họ)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailDTO> getProductDetail(@PathVariable Long id) {
        Long branchId = getBranchIdForCurrentVendor();
        ProductDetailDTO dto = vendorProductService.getProductDetailForBranch(id, branchId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * 🔄 3️⃣ Cập nhật trạng thái sản phẩm theo chi nhánh (ACTIVE / INACTIVE)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateProductStatus(
            @PathVariable Long id,
            @RequestParam String status // ACTIVE / INACTIVE
    ) {
        Long branchId = getBranchIdForCurrentVendor();
        vendorProductService.updateProductStatusForBranch(id, branchId, status);
        return ResponseEntity.ok().body("Cập nhật trạng thái sản phẩm thành công");
    }
}
