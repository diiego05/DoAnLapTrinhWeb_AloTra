package com.alotra.repository;

import com.alotra.entity.PromotionTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface PromotionTargetRepository extends JpaRepository<PromotionTarget, Long> {

    // 🟢 Lấy danh sách target theo Product ID
    List<PromotionTarget> findByProductId(Long productId);

    // 🟢 Lấy danh sách target theo Campaign ID (chuẩn cú pháp)
    List<PromotionTarget> findByCampaign_Id(Long campaignId);

    // 🔴 Xóa tất cả target theo Campaign ID
    @Transactional
    void deleteByCampaign_Id(Long campaignId);

    List<PromotionTarget> findByCampaignId(Long campaignId);
}
