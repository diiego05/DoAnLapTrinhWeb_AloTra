package com.alotra.repository;

import com.alotra.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findTopByProduct_IdOrderByPriceAsc(Long productId);

    // ✅ Chỉ giữ lại phương thức đúng kiểu Long
    List<ProductVariant> findByProduct_IdAndStatus(Long productId, String status);
}
