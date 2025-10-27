package com.alotra.repository;

import com.alotra.entity.CartItemTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface CartItemToppingRepository extends JpaRepository<CartItemTopping,Long> {
    List<CartItemTopping> findByCartItem_Id(Long cartItemId);
    
    @Modifying
    @Transactional
    void deleteByCartItem_Id(Long cartItemId);
    
    List<CartItemTopping> findByCartItemId(Long cartItemId);
}