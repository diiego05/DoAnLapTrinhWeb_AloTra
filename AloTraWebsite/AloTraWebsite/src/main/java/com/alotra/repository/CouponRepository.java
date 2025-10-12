package com.alotra.repository;

import com.alotra.entity.Coupon;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    @EntityGraph(attributePaths = "campaign")
    List<Coupon> findAll();
    List<Coupon> findByCampaignId(Long campaignId);
}
