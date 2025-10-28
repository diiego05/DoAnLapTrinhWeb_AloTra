package com.alotra.repository;

import com.alotra.entity.CartItemTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface CartItemToppingRepository extends JpaRepository<CartItemTopping,Long> {
    List<CartItemTopping> findByCartItem_Id(Long cartItemId);
    @Modifying
    @Transactional
    void deleteByCartItem_Id(Long cartItemId);
    List<CartItemTopping> findByCartItemId(Long cartItemId);
}
