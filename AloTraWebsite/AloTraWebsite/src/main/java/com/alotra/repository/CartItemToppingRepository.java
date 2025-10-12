package com.alotra.repository;

import com.alotra.entity.CartItemTopping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CartItemToppingRepository extends JpaRepository<CartItemTopping,Long> {
    List<CartItemTopping> findByCartItem_Id(Long cartItemId);
    void deleteByCartItem_Id(Long cartItemId);
}
