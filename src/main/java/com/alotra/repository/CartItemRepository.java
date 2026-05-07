package com.alotra.repository;

import com.alotra.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCart_IdAndVariant_Id(Long cartId, Long variantId);

    List<CartItem> findAllByCart_Id(Long cartId);

    void deleteByCart_Id(Long cartId);

    // ✅ Lọc các item theo userId từ bảng Cart
    List<CartItem> findAllByCart_UserIdAndIdIn(Long userId, List<Long> ids);
}
