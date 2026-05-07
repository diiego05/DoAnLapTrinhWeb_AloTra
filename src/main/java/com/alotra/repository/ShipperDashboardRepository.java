package com.alotra.repository;

import com.alotra.dto.dashboard.OrderByDateDTO;
import com.alotra.dto.dashboard.OrderStatusCountDTO;
import com.alotra.dto.dashboard.RecentOrderDTO;
import com.alotra.entity.ShippingAssignment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ShipperDashboardRepository extends JpaRepository<ShippingAssignment, Long> {

    @Query("""
        SELECT new com.alotra.dto.dashboard.OrderStatusCountDTO(sa.status, COUNT(sa))
        FROM ShippingAssignment sa
        WHERE sa.shipperId = :shipperId
        GROUP BY sa.status
    """)
    List<OrderStatusCountDTO> countOrdersByStatus(@Param("shipperId") Long shipperId);

    // ✅ Đếm đơn giao hôm nay (tránh lệch timezone)
    @Query("""
        SELECT COUNT(sa)
        FROM ShippingAssignment sa
        WHERE sa.shipperId = :shipperId
          AND sa.status = 'DELIVERED'
          AND CAST(sa.deliveredAt AS date) = CURRENT_DATE
    """)
    long countTodayDelivered(@Param("shipperId") Long shipperId);

    @Query("""
        SELECT COUNT(sa)
        FROM ShippingAssignment sa
        WHERE sa.shipperId = :shipperId
          AND sa.status = 'SHIPPING'
    """)
    long countInProgress(@Param("shipperId") Long shipperId);

    @Query("""
        SELECT COUNT(sa)
        FROM ShippingAssignment sa
        WHERE sa.shipperId = :shipperId
          AND sa.status = 'DELIVERED'
    """)
    long countTotalDelivered(@Param("shipperId") Long shipperId);

    @Query("""
    	    SELECT new com.alotra.dto.dashboard.RecentOrderDTO(
    	        o.id,
    	        o.code,
    	        u.fullName,
    	        o.deliveryAddress,
    	        (o.shippingFee * 0.7),
    	        sa.status,
    	        o.createdAt
    	    )
    	    FROM ShippingAssignment sa
    	    JOIN com.alotra.entity.Order o ON sa.orderId = o.id
    	    JOIN com.alotra.entity.User u ON o.userId = u.id
    	    WHERE sa.shipperId = :shipperId
    	    ORDER BY o.createdAt DESC
    	""")
    	List<RecentOrderDTO> findRecentOrders(@Param("shipperId") Long shipperId, Pageable pageable);


    // ✅ Nhóm đơn theo ngày (gom theo ngày, tránh lệch giờ)
    @Query("""
        SELECT new com.alotra.dto.dashboard.OrderByDateDTO(
            CAST(o.createdAt AS date),
            COUNT(o),
            SUM(o.shippingFee * 0.7)
        )
        FROM ShippingAssignment sa
        JOIN com.alotra.entity.Order o ON sa.orderId = o.id
        WHERE sa.shipperId = :shipperId
          AND sa.status = 'DELIVERED'
          AND o.createdAt BETWEEN :from AND :to
        GROUP BY CAST(o.createdAt AS date)
        ORDER BY CAST(o.createdAt AS date)
    """)
    List<OrderByDateDTO> findOrdersByDate(@Param("shipperId") Long shipperId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    // ✅ Tổng doanh thu = 70% phí vận chuyển của đơn đã giao
    @Query("""
        SELECT COALESCE(SUM(o.shippingFee * 0.7), 0)
        FROM ShippingAssignment sa
        JOIN com.alotra.entity.Order o ON sa.orderId = o.id
        WHERE sa.shipperId = :shipperId
          AND sa.status = 'DELIVERED'
    """)
    BigDecimal sumTotalEarnings(@Param("shipperId") Long shipperId);

    // ✅ Doanh thu hôm nay
    @Query("""
        SELECT COALESCE(SUM(o.shippingFee * 0.7), 0)
        FROM ShippingAssignment sa
        JOIN com.alotra.entity.Order o ON sa.orderId = o.id
        WHERE sa.shipperId = :shipperId
          AND sa.status = 'DELIVERED'
          AND CAST(sa.deliveredAt AS date) = CURRENT_DATE
    """)
    BigDecimal sumTodayEarnings(@Param("shipperId") Long shipperId);
}
