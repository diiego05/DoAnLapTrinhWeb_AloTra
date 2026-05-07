package com.alotra.controller.api;

import com.alotra.dto.WishlistResponse;
import com.alotra.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // 📜 Lấy danh sách yêu thích của user hiện tại
    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getAllForUser() {
        return ResponseEntity.ok(wishlistService.getAllForCurrentUser());
    }

    // ➕ Thêm sản phẩm vào yêu thích
    @PostMapping("/{productId}")
    public ResponseEntity<?> addToWishlist(@PathVariable Long productId) {
        wishlistService.addToWishlist(productId);
        return ResponseEntity.ok().build();
    }

    // ❌ Xóa sản phẩm khỏi yêu thích
    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long productId) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.ok().build();
    }
}
