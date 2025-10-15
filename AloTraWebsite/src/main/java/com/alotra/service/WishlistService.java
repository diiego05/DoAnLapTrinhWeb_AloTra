/*package com.alotra.service;

import com.alotra.dto.WishlistResponse;
import com.alotra.entity.*;
import com.alotra.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepo;
    private final ProductRepository productRepo;
    private final UserService userService; // Lấy user hiện tại từ JWT

    @PersistenceContext
    private EntityManager em;

    // 🔹 Lấy toàn bộ danh sách yêu thích của user hiện tại
    public List<WishlistResponse> getAllForCurrentUser() {
        User currentUser = userService.getCurrentUserOrThrow();
        List<Wishlist> list = wishlistRepo.findByUserId(currentUser.getId());

        return list.stream().map(w -> {
            Product p = w.getProduct();
            if (p == null) return null;

            // 🖼️ Lấy thumbnail (ưu tiên ảnh chính nếu có)
            String thumbnail = p.getMedia() != null && !p.getMedia().isEmpty()
                    ? p.getMedia().stream()
                    		.filter(ProductMedia::isPrimary)

                        .findFirst()
                        .orElse(p.getMedia().iterator().next())
                        .getUrl()
                    : null;

            // 💰 Lấy giá đầu tiên (size đầu tiên)
            BigDecimal price = p.getVariants() != null && !p.getVariants().isEmpty()
                    ? p.getVariants().iterator().next().getPrice()
                    : null;

            return new WishlistResponse(
                    p.getId(),
                    p.getName(),
                    price,
                    thumbnail
            );
        }).filter(r -> r != null).toList();
    }

    // ➕ Thêm sản phẩm vào danh sách yêu thích
    public void addToWishlist(Long productId) {
        User currentUser = userService.getCurrentUserOrThrow();

        if (!productRepo.existsById(productId))
            throw new IllegalArgumentException("Sản phẩm không tồn tại");

        if (wishlistRepo.existsByUserIdAndProductId(currentUser.getId(), productId))
            return; // tránh thêm trùng

        wishlistRepo.save(new Wishlist(currentUser.getId(), productId));
        em.flush();
    }

    // ❌ Xóa sản phẩm khỏi danh sách yêu thích
    public void removeFromWishlist(Long productId) {
        User currentUser = userService.getCurrentUserOrThrow();
        wishlistRepo.deleteByUserIdAndProductId(currentUser.getId(), productId);
        em.flush();
    }
}*/
package com.alotra.service;

import com.alotra.dto.ProductVariantDTO;
import com.alotra.dto.WishlistResponse;
import com.alotra.entity.*;
import com.alotra.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepo;
    private final ProductRepository productRepo;
    private final UserService userService;
    private final ProductVariantService productVariantService;  // ✅ thêm service này để lấy giá khuyến mãi

    @PersistenceContext
    private EntityManager em;

    // 🔹 Lấy toàn bộ danh sách yêu thích của user hiện tại
    public List<WishlistResponse> getAllForCurrentUser() {
        User currentUser = userService.getCurrentUserOrThrow();
        List<Wishlist> list = wishlistRepo.findByUserId(currentUser.getId());

        return list.stream().map(w -> {
            Product p = w.getProduct();
            if (p == null) return null;

            // 🖼️ Lấy thumbnail (ưu tiên ảnh chính nếu có)
            String thumbnail = p.getMedia() != null && !p.getMedia().isEmpty()
                    ? p.getMedia().stream()
                            .filter(ProductMedia::isPrimary)
                            .findFirst()
                            .orElse(p.getMedia().iterator().next())
                            .getUrl()
                    : null;

            // 💰 Lấy giá sau giảm từ ProductVariantService
            BigDecimal price = null;
            List<ProductVariantDTO> variantDTOs = productVariantService.getVariantDTOsByProductId(p.getId());

            if (variantDTOs != null && !variantDTOs.isEmpty()) {
                // ✅ lấy giá rẻ nhất trong các biến thể sau giảm
                price = variantDTOs.stream()
                        .map(ProductVariantDTO::getDiscountedPrice)
                        .min(Comparator.naturalOrder())
                        .orElse(null);
            }

            return new WishlistResponse(
                    p.getId(),
                    p.getName(),
                    price,
                    thumbnail
            );
        }).filter(r -> r != null).toList();
    }

    // ➕ Thêm sản phẩm vào danh sách yêu thích
    public void addToWishlist(Long productId) {
        User currentUser = userService.getCurrentUserOrThrow();

        if (!productRepo.existsById(productId))
            throw new IllegalArgumentException("Sản phẩm không tồn tại");

        if (wishlistRepo.existsByUserIdAndProductId(currentUser.getId(), productId))
            return; // tránh thêm trùng

        wishlistRepo.save(new Wishlist(currentUser.getId(), productId));
        em.flush();
    }

    // ❌ Xóa sản phẩm khỏi danh sách yêu thích
    public void removeFromWishlist(Long productId) {
        User currentUser = userService.getCurrentUserOrThrow();
        wishlistRepo.deleteByUserIdAndProductId(currentUser.getId(), productId);
        em.flush();
    }
}

