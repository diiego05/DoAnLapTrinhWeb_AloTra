package com.alotra.repository;

import com.alotra.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CartItemRepository extends JpaRepository<CartItem,Long> {
    Optional<CartItem> findByCart_IdAndVariant_Id(Long cartId,Long variantId);
    List<CartItem> findAllByCart_Id(Long cartId);
    void deleteByCart_Id(Long cartId);
}
