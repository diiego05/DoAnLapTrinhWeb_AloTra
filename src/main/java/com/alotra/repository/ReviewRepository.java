package com.alotra.repository;

import com.alotra.entity.Review;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderItemId(Long orderItemId);

    List<Review> findByProduct_IdOrderByCreatedAtDesc(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double avgRatingByProductId(Long productId);

    int countByProduct_Id(Long productId);

    Optional<Review> findByOrderItemId(Long orderItemId);

    Page<Review> findByProductId(Long productId, Pageable pageable);



    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Optional<Double> calculateAverageRating(@Param("productId") Long productId);
}
