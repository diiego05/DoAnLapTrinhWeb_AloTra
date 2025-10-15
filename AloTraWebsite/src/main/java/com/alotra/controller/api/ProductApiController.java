package com.alotra.controller.api;

import com.alotra.dto.ProductFormDTO;
import com.alotra.dto.ProductListDTO;
import com.alotra.dto.ProductVariantDTO;
import com.alotra.dto.ProductDetailDTO;
import com.alotra.dto.ProductSummaryDTO;
import com.alotra.service.ProductService;
import com.alotra.service.ProductVariantService;
import com.alotra.repository.CategoryRepository;
import com.alotra.repository.SizeRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    @Autowired private ProductService productService;
    @Autowired private ProductVariantService productVariantService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SizeRepository sizeRepository;

    // ============================================================
    // 🔸 ADMIN
    // ============================================================

    @GetMapping
    public ResponseEntity<List<ProductListDTO>> getProducts(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        return ResponseEntity.ok(productService.searchProductsForAdmin(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            ProductDetailDTO product = productService.getProductDetailById(id);
            if (product == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy sản phẩm với ID " + id);
            }
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải sản phẩm: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<?> getProductDetail(@PathVariable Long id) {
        try {
            ProductDetailDTO detail = productService.getProductDetailById(id);
            if (detail == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Không tìm thấy sản phẩm với ID " + id);
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải chi tiết sản phẩm: " + e.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @Valid @RequestPart("product") ProductFormDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        try {
            productService.createProduct(dto, files);
            return new ResponseEntity<>("Tạo sản phẩm thành công!", HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi tạo sản phẩm: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @Valid @RequestPart("product") ProductFormDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        try {
            productService.updateProduct(id, dto, files);
            return ResponseEntity.ok("Cập nhật sản phẩm thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi cập nhật sản phẩm: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không thể xóa sản phẩm: " + e.getMessage());
        }
    }

    @GetMapping("/aux-data")
    public ResponseEntity<Map<String, Object>> getAuxiliaryData() {
        Map<String, Object> data = new HashMap<>();
        data.put("categories", categoryRepository.findAll());
        data.put("sizes", sizeRepository.findAll());
        return ResponseEntity.ok(data);
    }

    // ============================================================
    // 🛒 PUBLIC - Người mua
    // ============================================================

    /**
     * 🆕 Lấy danh sách sản phẩm active (người mua)
     */
    @GetMapping("/public")
    public ResponseEntity<List<ProductSummaryDTO>> getActiveProducts(
            @RequestParam(required = false) String categorySlug) {
        return ResponseEntity.ok(productService.findActiveProducts(categorySlug));
    }

    /**
     * 🆕 Lấy danh sách sản phẩm có phân trang (nếu dùng nút "Xem thêm")
     */
    @GetMapping("/public/page")
    public ResponseEntity<Page<ProductSummaryDTO>> getActiveProductsPaged(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.findActiveProducts(categorySlug, pageable));
    }

    // ============================================================
    // 🧾 BIẾN THỂ - GIẢM GIÁ THEO SIZE
    // ============================================================

    /**
     * 🆕 Lấy danh sách biến thể có áp dụng giảm giá
     * ➝ Dùng cho modal chọn size trên trang chi tiết sản phẩm
     */
    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariantDTO>> getVariantsByProductId(@PathVariable Long productId) {
        try {
            List<ProductVariantDTO> variants = productVariantService.getVariantDTOsByProductId(productId);
            return ResponseEntity.ok(variants);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * (Tùy chọn) API này chỉ nên giữ nếu bạn cần debug entity gốc
     */
    @GetMapping("/{productId}/variants/raw")
    public ResponseEntity<?> getVariantsByProductRaw(@PathVariable Long productId) {
        return ResponseEntity.ok(productVariantService.getActiveVariantsByProductId(productId));
    }

    /**
     * 🆕 API: Lấy top 10 sản phẩm có mức giảm giá cao nhất
     */
    @GetMapping("/public/top-discount")
    public ResponseEntity<List<ProductSummaryDTO>> getTopDiscountProducts() {
        try {
            List<ProductSummaryDTO> topDiscounted = productService.getTopDiscountProducts(10);
            return ResponseEntity.ok(topDiscounted);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

}
