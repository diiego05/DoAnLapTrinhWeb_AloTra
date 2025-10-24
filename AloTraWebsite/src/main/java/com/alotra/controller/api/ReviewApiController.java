package com.alotra.controller.api;

import com.alotra.dto.ReviewRequestDTO;
import com.alotra.dto.ReviewResponseDTO;
import com.alotra.entity.Review;
import com.alotra.repository.ReviewRepository;
import com.alotra.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@CrossOrigin // 👈 tránh lỗi CORS khi front-end chạy ở domain khác
public class ReviewApiController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    /* =========================== 📝 TẠO REVIEW MỚI =========================== */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDTO> createReview(
            @RequestPart("review") ReviewRequestDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        log.info("🆕 POST /api/reviews -> orderItemId={}, productId={}",
                dto.getOrderItemId(), dto.getProductId());

        Review saved = reviewService.createReview(dto, files);
        return ResponseEntity.ok(toDTO(saved));
    }

    /* =========================== ✏️ CẬP NHẬT REVIEW =========================== */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable Long id,
            @RequestPart("review") ReviewRequestDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        log.info("✏️ PUT /api/reviews/{} -> Cập nhật review", id);

        Review updated = reviewService.updateReview(id, dto, files);
        return ResponseEntity.ok(toDTO(updated));
    }

    /* =========================== 📦 LẤY REVIEW THEO PRODUCT - PHÂN TRANG =========================== */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getReviewsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        log.info("📥 GET /api/reviews/product/{}?page={}&size={}", productId, page, size);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByProductId(productId, pageable);

        double avgRating = reviewRepository.calculateAverageRating(productId).orElse(0.0);

        Map<String, Object> response = new HashMap<>();
        response.put("total", reviewPage.getTotalElements());
        response.put("averageRating", avgRating);
        response.put("reviews", reviewPage.getContent().stream().map(this::toDTO).toList());
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("currentPage", page);

        return ResponseEntity.ok(response);
    }

    /* =========================== 📦 LẤY REVIEW THEO ORDER ITEM =========================== */
    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<ReviewResponseDTO> getByOrderItemId(@PathVariable Long orderItemId) {
        log.info("📥 GET /api/reviews/order-item/{}", orderItemId);
        return reviewRepository.findByOrderItemId(orderItemId)
                .map(review -> ResponseEntity.ok(toDTO(review)))
                .orElse(ResponseEntity.notFound().build());
    }

    /* =========================== 📦 LẤY REVIEW THEO ID =========================== */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> getReviewById(@PathVariable Long id) {
        log.info("📥 GET /api/reviews/{}", id);
        return reviewRepository.findById(id)
                .map(review -> ResponseEntity.ok(toDTO(review)))
                .orElse(ResponseEntity.notFound().build());
    }

    /* =========================== 📦 LẤY TOÀN BỘ REVIEW (KHÔNG PHÂN TRANG) =========================== */
    @GetMapping("/product/{productId}/all")
    public ResponseEntity<Map<String, Object>> getAllReviewsForProduct(@PathVariable Long productId) {
        log.info("📥 GET /api/reviews/product/{}/all", productId);

        var reviews = reviewService.getReviewsByProduct(productId);
        var sortedReviews = reviews.stream()
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .map(this::toDTO)
                .collect(Collectors.toList());

        double avgRating = reviews.isEmpty()
                ? 0
                : reviews.stream().mapToInt(Review::getRating).average().orElse(0);

        Map<String, Object> response = new HashMap<>();
        response.put("total", reviews.size());
        response.put("averageRating", avgRating);
        response.put("reviews", sortedReviews);

        return ResponseEntity.ok(response);
    }

    /* =========================== 🧭 MAP ENTITY -> DTO =========================== */
    private ReviewResponseDTO toDTO(Review review) {
        Long productId = (review.getProduct() != null) ? review.getProduct().getId() : null;
        String userName = (review.getUser() != null) ? review.getUser().getFullName() : "Ẩn danh";

        List<String> mediaUrls = (review.getMediaList() != null)
                ? review.getMediaList().stream()
                .map(m -> m.getUrl())
                .collect(Collectors.toList())
                : List.of();

        return new ReviewResponseDTO(
                review.getId(),
                review.getOrderItemId(),
                productId,
                review.getContent(),
                review.getRating(),
                userName,
                mediaUrls
        );
    }
}
