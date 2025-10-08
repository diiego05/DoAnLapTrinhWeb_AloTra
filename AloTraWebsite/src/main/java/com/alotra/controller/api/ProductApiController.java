package com.alotra.controller.api;

import com.alotra.dto.ProductFormDTO;
import com.alotra.dto.ProductListDTO;
import com.alotra.dto.ProductDetailDTO;
import com.alotra.repository.CategoryRepository;
import com.alotra.repository.SizeRepository;
import com.alotra.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SizeRepository sizeRepository;

    /**
     * 🟢 Lấy danh sách sản phẩm (có hỗ trợ tìm kiếm)
     */
    @GetMapping
    public ResponseEntity<List<ProductListDTO>> getProducts(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        List<ProductListDTO> products = productService.searchProductsForAdmin(keyword);
        return ResponseEntity.ok(products);
    }

    /**
     * 🟢 Lấy chi tiết sản phẩm theo ID (phục vụ nút "Sửa")
     */
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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải sản phẩm: " + e.getMessage());
        }
    }

    /**
     * 🟢 Lấy chi tiết sản phẩm (dành riêng cho nút "Xem chi tiết")
     * — bao gồm mô tả, hình ảnh, biến thể, trạng thái, danh mục.
     */
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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tải chi tiết sản phẩm: " + e.getMessage());
        }
    }

    /**
     * 🟢 Tạo mới sản phẩm (POST /api/products)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @Valid @RequestPart("product") ProductFormDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        try {
            productService.createProduct(dto, files);
            return new ResponseEntity<>("Tạo sản phẩm thành công!", HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi khi tạo sản phẩm: " + e.getMessage());
        }
    }

    /**
     * 🟡 Cập nhật sản phẩm (PUT /api/products/{id})
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @Valid @RequestPart("product") ProductFormDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        try {
            productService.updateProduct(id, dto, files);
            return ResponseEntity.ok("Cập nhật sản phẩm thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi khi cập nhật sản phẩm: " + e.getMessage());
        }
    }

    /**
     * 🔴 Xóa sản phẩm theo ID (DELETE /api/products/{id})
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không thể xóa sản phẩm: " + e.getMessage());
        }
    }

    /**
     * 🟢 Lấy dữ liệu phụ trợ (Category & Size) cho form thêm/sửa
     */
    @GetMapping("/aux-data")
    public ResponseEntity<Map<String, Object>> getAuxiliaryData() {
        Map<String, Object> data = new HashMap<>();
        data.put("categories", categoryRepository.findAll());
        data.put("sizes", sizeRepository.findAll());
        return ResponseEntity.ok(data);
    }
}
