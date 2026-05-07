package com.alotra.repository;

import com.alotra.entity.PromotionalCampaign;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;  //

import java.time.LocalDateTime;
import java.util.List;
/*
public interface PromotionalCampaignRepository extends JpaRepository<PromotionalCampaign, Long> {

    // 🟢 Lấy danh sách chiến dịch đang hoạt động
    @Query("SELECT c FROM PromotionalCampaign c " +
           "WHERE c.status = 'ACTIVE' AND c.startAt <= :now AND c.endAt >= :now")
    List<PromotionalCampaign> findActiveCampaigns(@Param("now") LocalDateTime now);

    // 🟡 Lấy danh sách chiến dịch đã hết hạn
    @Query("SELECT c FROM PromotionalCampaign c " +
           "WHERE c.endAt < :now")
    List<PromotionalCampaign> findExpiredCampaigns(@Param("now") LocalDateTime now);

    // 🟠 Lấy danh sách chiến dịch sắp diễn ra
    @Query("SELECT c FROM PromotionalCampaign c " +
           "WHERE c.startAt > :now")
    List<PromotionalCampaign> findUpcomingCampaigns(@Param("now") LocalDateTime now);

    // 🔸 Tăng lượt xem khi người dùng truy cập trang chi tiết
    @Modifying
    @Transactional
    @Query("UPDATE PromotionalCampaign c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // 🔹 Lấy danh sách chiến dịch theo view cao nhất (top n)
    @Query("SELECT c FROM PromotionalCampaign c ORDER BY c.viewCount DESC")
    List<PromotionalCampaign> findTopByViewCount(Pageable pageable);
}*/


public interface PromotionalCampaignRepository extends JpaRepository<PromotionalCampaign, Long> {

    // 🟢 Lấy danh sách chiến dịch đang hoạt động
    @Query("""
        SELECT c FROM PromotionalCampaign c
        WHERE c.status = 'ACTIVE'
        AND c.startAt <= :now
        AND c.endAt >= :now
    """)
    List<PromotionalCampaign> findActiveCampaigns(@Param("now") LocalDateTime now);

    // 🟡 Lấy danh sách chiến dịch đã hết hạn
    @Query("""
        SELECT c FROM PromotionalCampaign c
        WHERE c.endAt < :now
    """)
    List<PromotionalCampaign> findExpiredCampaigns(@Param("now") LocalDateTime now);

    // 🟠 Lấy danh sách chiến dịch sắp diễn ra
    @Query("""
        SELECT c FROM PromotionalCampaign c
        WHERE c.startAt > :now
    """)
    List<PromotionalCampaign> findUpcomingCampaigns(@Param("now") LocalDateTime now);

    // 🔸 Tăng lượt xem khi người dùng truy cập trang chi tiết
    @Modifying
    @Transactional
    @Query("UPDATE PromotionalCampaign c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // 🔹 Lấy danh sách chiến dịch theo view cao nhất (Top N)
    @Query("""
        SELECT c FROM PromotionalCampaign c
        ORDER BY c.viewCount DESC
    """)
    List<PromotionalCampaign> findTopByViewCount(Pageable pageable);

    // 🧮 Đếm số chiến dịch theo trạng thái
    long countByStatus(PromotionalCampaign.CampaignStatus status);

    // 📊 Lấy chiến dịch theo trạng thái cụ thể
    List<PromotionalCampaign> findByStatus(String status);

    // 📅 Lấy chiến dịch trong khoảng thời gian (phục vụ thống kê theo thời gian)
    @Query("""
        SELECT c FROM PromotionalCampaign c
        WHERE c.startAt BETWEEN :from AND :to
           OR c.endAt BETWEEN :from AND :to
    """)
    List<PromotionalCampaign> findCampaignsInPeriod(@Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);
}

