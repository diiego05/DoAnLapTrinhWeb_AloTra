package com.alotra.repository;

import com.alotra.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {
    Optional<ProductMedia> findFirstByProduct_IdAndIsPrimaryTrueOrderByIdAsc(Long productId);
    Optional<ProductMedia> findFirstByProduct_IdOrderByIdAsc(Long productId);
}
