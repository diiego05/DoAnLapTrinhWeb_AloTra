package com.alotra.repository;

import com.alotra.entity.ReviewMedia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {
	List<ReviewMedia> findByReviewId(Long reviewId);
}
