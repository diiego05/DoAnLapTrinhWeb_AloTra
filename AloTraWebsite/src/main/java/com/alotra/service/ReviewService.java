package com.alotra.service;

import com.alotra.dto.ReviewRequestDTO;
import com.alotra.entity.*;
import com.alotra.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewMediaRepository reviewMediaRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private UserRepository userRepository;

    /* =========================== 1️⃣ TẠO REVIEW =========================== */
    @Transactional
    public Review createReview(ReviewRequestDTO dto, List<MultipartFile> mediaFiles) {
        Long currentUserId = userService.getCurrentUserId();

        OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong đơn hàng"));

        if (!orderItem.getOrder().getUserId().equals(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền đánh giá sản phẩm này");
        }

        if (reviewRepository.existsByOrderItemId(dto.getOrderItemId())) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi");
        }

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Review review = new Review();
        review.setOrderItemId(dto.getOrderItemId());
        review.setUser(user);
        review.setProduct(product);
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        Review savedReview = reviewRepository.save(review);

        // Upload media nếu có
        uploadMedia(mediaFiles, savedReview);

        updateProductRating(product);

        return savedReview;
    }

    /* =========================== 2️⃣ CẬP NHẬT REVIEW =========================== */
    @Transactional
    public Review updateReview(Long reviewId, ReviewRequestDTO dto, List<MultipartFile> mediaFiles) {
        Long currentUserId = userService.getCurrentUserId();

        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        if (!existing.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa đánh giá này");}

        existing.setRating(dto.getRating());
        existing.setContent(dto.getContent());

        Review savedReview = reviewRepository.save(existing);

        // Xoá media cũ (nếu có)
        List<ReviewMedia> oldMedia = reviewMediaRepository.findByReviewId(reviewId);
        if (!oldMedia.isEmpty()) {
            reviewMediaRepository.deleteAll(oldMedia);
        }

        // Upload media mới (nếu có)
        uploadMedia(mediaFiles, savedReview);

        // Cập nhật lại điểm trung bình sản phẩm
        updateProductRating(existing.getProduct());

        return savedReview;
    }

    /* =========================== 3️⃣ LẤY REVIEW THEO SẢN PHẨM =========================== */
    public List<Review> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId);
    }

    /* =========================== 📌 HÀM PHỤ TRỢ =========================== */
    private void uploadMedia(List<MultipartFile> mediaFiles, Review review) {
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MultipartFile file : mediaFiles) {
                String url = cloudinaryService.uploadMediaFile(file);
                ReviewMedia rm = new ReviewMedia();
                rm.setReview(review);
                rm.setUrl(url);
                rm.setMediaType(file.getContentType() != null && file.getContentType().startsWith("video")
                        ? "VIDEO" : "IMAGE");
                reviewMediaRepository.save(rm);
            }
        }
    }

    private void updateProductRating(Product product) {
        Double avg = reviewRepository.avgRatingByProductId(product.getId());
        int count = reviewRepository.countByProduct_Id(product.getId());

        product.setRatingAvg(
            avg != null ? BigDecimal.valueOf(avg) : BigDecimal.ZERO
        );
        product.setRatingCount(count);

        productRepository.save(product);
    }

}
