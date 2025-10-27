/*
package com.alotra.repository;

import com.alotra.dto.dashboard.*;
import com.alotra.entity.Order;
import com.alotra.enums.OrderStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByCode(String code);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByOrderByCreatedAtDesc();

    // ✅ Admin - lọc theo branch
    List<Order> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    // ✅ Admin - lọc theo status
    List<Order> findByStatusOrderByCreatedAtDesc(String status);

    // ✅ Admin - lọc theo branch + status
    List<Order> findByBranchIdAndStatusOrderByCreatedAtDesc(Long branchId, String status);

    // ============================
    // 📦 VENDOR TRUY VẤN ĐƠN HÀNG
    // ============================
    @Query(value = """
        SELECT o.* FROM Orders o
        JOIN Branches b ON o.BranchId = b.Id
        WHERE b.ManagerId = :vendorId
        ORDER BY o.CreatedAt DESC
    """, nativeQuery = true)
    List<Order> findOrdersByVendorId(@Param("vendorId") Long vendorId);

    @Query(value = """
        SELECT o.* FROM Orders o
        JOIN Branches b ON o.BranchId = b.Id
        WHERE b.ManagerId = :vendorId AND o.Status = :status
        ORDER BY o.CreatedAt DESC
    """, nativeQuery = true)
    List<Order> findOrdersByVendorIdAndStatus(@Param("vendorId") Long vendorId,
                                              @Param("status") String status);

    // ============================
    // 📊 DASHBOARD
    // ============================
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.status = 'COMPLETED'")
    BigDecimal sumTotalCompleted();

    // 📈 Doanh thu theo ngày (fix: dùng LocalDateTime)
    @Query("""
        SELECT new com.alotra.dto.dashboard.RevenuePointDTO(
            CAST(o.createdAt AS LocalDate),
            SUM(o.total)
        )
        FROM Order o
        WHERE o.status = 'COMPLETED' AND o.createdAt BETWEEN :from AND :to
        GROUP BY CAST(o.createdAt AS LocalDate)
        ORDER BY CAST(o.createdAt AS LocalDate)
    """)
    List<RevenuePointDTO> revenueByDateRange(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    // 🧾 Biểu đồ trạng thái đơn hàng
    @Query("""
        SELECT new com.alotra.dto.dashboard.OrderStatusCountDTO(
            o.status,
            COUNT(o)
        )
        FROM Order o
        WHERE o.createdAt BETWEEN :from AND :to
        GROUP BY o.status
    """)
    List<OrderStatusCountDTO> countOrdersByStatus(@Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);

    // 🏪 Top chi nhánh (native)
    @Query(value = """
        SELECT b.Name AS branchName, SUM(o.Total) AS totalRevenue
        FROM Orders o
        JOIN Branches b ON o.BranchId = b.Id
        WHERE o.Status = 'COMPLETED'
        AND o.CreatedAt BETWEEN :from AND :to
        GROUP BY b.Name
        ORDER BY totalRevenue DESC
    """, nativeQuery = true)
    List<Object[]> findTopBranchesNative(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    // 🧾 Đơn hàng mới nhất (native)
    @Query(value = """
        SELECT o.Code, u.FullName, o.Total, o.Status, o.CreatedAt
        FROM Orders o
        JOIN Users u ON o.UserId = u.Id
        ORDER BY o.CreatedAt DESC
    """, nativeQuery = true)
    List<Object[]> findLatestOrdersNative(org.springframework.data.domain.Pageable pageable);

    default List<Object[]> findLatestOrdersNative(int limit) {
        return findLatestOrdersNative(PageRequest.of(0, limit));
    }


 // OrderRepository.java (thêm vào)
    @Query("SELECT b.id FROM Branch b WHERE b.manager.id = :vendorId")
    Long findBranchIdByVendor(@Param("vendorId") Long vendorId);


    @Query("SELECT COUNT(o) FROM Order o WHERE o.branchId = :branchId AND o.status = 'COMPLETED'")
    long countByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.branchId = :branchId AND o.status = 'COMPLETED'")
    BigDecimal sumTotalByBranchCompleted(@Param("branchId") Long branchId);

    @Query("""
        SELECT new com.alotra.dto.dashboard.RevenuePointDTO(
            CAST(o.createdAt AS LocalDate),
            SUM(o.total)
        )
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.status = 'COMPLETED'
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY CAST(o.createdAt AS LocalDate)
        ORDER BY CAST(o.createdAt AS LocalDate)
    """)
    List<RevenuePointDTO> revenueByDateRangeAndBranch(
            @Param("branchId") Long branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
        SELECT new com.alotra.dto.dashboard.OrderStatusCountDTO(
            o.status,
            COUNT(o)
        )
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY o.status
    """)
    List<OrderStatusCountDTO> countOrdersByStatusAndBranch(
            @Param("branchId") Long branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT o.Code, u.FullName, o.Total, o.Status, o.CreatedAt
        FROM Orders o
        JOIN Users u ON o.UserId = u.Id
        WHERE o.BranchId = :branchId
        ORDER BY o.CreatedAt DESC
        OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
    """, nativeQuery = true)
    List<Object[]> findLatestOrdersByBranch(@Param("branchId") Long branchId, @Param("limit") int limit);




    // 🧑‍🤝‍🧑 Top khách hàng theo chi nhánh (Vendor)
    @Query("""
        SELECT new com.alotra.dto.dashboard.TopCustomerDTO(
            u.fullName,
            COUNT(o.id),
            SUM(o.total)
        )
        FROM Order o
        JOIN User u ON o.userId = u.id
        WHERE o.branchId = :branchId
        AND o.status = 'COMPLETED'
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY u.fullName
        ORDER BY SUM(o.total) DESC
    """)
    List<TopCustomerDTO> findTopCustomersByBranch(
            @Param("branchId") Long branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);




}*/
package com.alotra.repository;

import com.alotra.dto.*;
import com.alotra.dto.dashboard.*;
import com.alotra.entity.Order;
import com.alotra.enums.OrderStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ==========================
    // 🔸 TRUY VẤN CƠ BẢN
    // ==========================
    Optional<Order> findByCode(String code);
    List<Order> findByUserId(Long userId);
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    Optional<Order> findByIdAndUserId(Long id, Long userId);
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByBranchIdOrderByCreatedAtDesc(Long branchId);
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
    List<Order> findByBranchIdAndStatusOrderByCreatedAtDesc(Long branchId, String status);

    // ==========================
    // 🧑‍💼 VENDOR TRUY VẤN ĐƠN
    // ==========================
    @Query(value = """
        SELECT o.* FROM Orders o
        JOIN Branches b ON o.BranchId = b.Id
        WHERE b.ManagerId = :vendorId
        ORDER BY o.CreatedAt DESC
    """, nativeQuery = true)
    List<Order> findOrdersByVendorId(@Param("vendorId") Long vendorId);

    @Query(value = """
        SELECT o.* FROM Orders o
        JOIN Branches b ON o.BranchId = b.Id
        WHERE b.ManagerId = :vendorId AND o.Status = :status
        ORDER BY o.CreatedAt DESC
    """, nativeQuery = true)
    List<Order> findOrdersByVendorIdAndStatus(@Param("vendorId") Long vendorId,
                                              @Param("status") String status);

    // ==========================
    // 📊 DASHBOARD ADMIN
    // ==========================
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.status = 'COMPLETED'")
    BigDecimal sumTotalCompleted();

    @Query("""
        SELECT new com.alotra.dto.dashboard.RevenuePointDTO(
            CAST(o.createdAt AS LocalDate),
            SUM(o.total)
        )
        FROM Order o
        WHERE o.status = 'COMPLETED' AND o.createdAt BETWEEN :from AND :to
        GROUP BY CAST(o.createdAt AS LocalDate)
        ORDER BY CAST(o.createdAt AS LocalDate)
    """)
    List<RevenuePointDTO> revenueByDateRange(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    @Query("""
        SELECT new com.alotra.dto.dashboard.OrderStatusCountDTO(
            o.status,
            COUNT(o)
        )
        FROM Order o
        WHERE o.createdAt BETWEEN :from AND :to
        GROUP BY o.status
    """)
    List<OrderStatusCountDTO> countOrdersByStatus(@Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT b.Name AS branchName, SUM(o.Total) AS totalRevenue
        FROM Orders o
        JOIN Branches b ON o.BranchId = b.Id
        WHERE o.Status = 'COMPLETED'
        AND o.CreatedAt BETWEEN :from AND :to
        GROUP BY b.Name
        ORDER BY totalRevenue DESC
    """, nativeQuery = true)
    List<Object[]> findTopBranchesNative(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT o.Code, u.FullName, o.Total, o.Status, o.CreatedAt
        FROM Orders o
        JOIN Users u ON o.UserId = u.Id
        ORDER BY o.CreatedAt DESC
    """, nativeQuery = true)
    List<Object[]> findLatestOrdersNative(org.springframework.data.domain.Pageable pageable);

    default List<Object[]> findLatestOrdersNative(int limit) {
        return findLatestOrdersNative(PageRequest.of(0, limit));
    }

    // ==========================
    // 🏪 DASHBOARD VENDOR
    // ==========================
    @Query("SELECT b.id FROM Branch b WHERE b.manager.id = :vendorId")
    Long findBranchIdByVendor(@Param("vendorId") Long vendorId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.branchId = :branchId AND o.status = 'COMPLETED'")
    long countByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.branchId = :branchId AND o.status = 'COMPLETED'")
    BigDecimal sumTotalByBranchCompleted(@Param("branchId") Long branchId);

    @Query("""
        SELECT new com.alotra.dto.dashboard.RevenuePointDTO(
            CAST(o.createdAt AS LocalDate),
            SUM(o.total)
        )
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.status = 'COMPLETED'
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY CAST(o.createdAt AS LocalDate)
        ORDER BY CAST(o.createdAt AS LocalDate)
    """)
    List<RevenuePointDTO> revenueByDateRangeAndBranch(
            @Param("branchId") Long branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
        SELECT new com.alotra.dto.dashboard.OrderStatusCountDTO(
            o.status,
            COUNT(o)
        )
        FROM Order o
        WHERE o.branchId = :branchId
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY o.status
    """)
    List<OrderStatusCountDTO> countOrdersByStatusAndBranch(
            @Param("branchId") Long branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT o.Code, u.FullName, o.Total, o.Status, o.CreatedAt
        FROM Orders o
        JOIN Users u ON o.UserId = u.Id
        WHERE o.BranchId = :branchId
        ORDER BY o.CreatedAt DESC
        OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY
    """, nativeQuery = true)
    List<Object[]> findLatestOrdersByBranch(@Param("branchId") Long branchId, @Param("limit") int limit);

    @Query("""
        SELECT new com.alotra.dto.dashboard.TopCustomerDTO(
            u.fullName,
            COUNT(o.id),
            SUM(o.total)
        )
        FROM Order o
        JOIN User u ON o.userId = u.id
        WHERE o.branchId = :branchId
        AND o.status = 'COMPLETED'
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY u.fullName
        ORDER BY SUM(o.total) DESC
    """)
    List<TopCustomerDTO> findTopCustomersByBranch(
            @Param("branchId") Long branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // ==========================
    // 🚚 DASHBOARD SHIPPER
    // ==========================
    @Query("""
        SELECT COUNT(o) FROM Order o
        JOIN ShippingAssignment sa ON sa.orderId = o.id
        WHERE sa.shipperId = :shipperId
        AND o.status = 'COMPLETED'
        AND o.createdAt BETWEEN :from AND :to
    """)
    long countCompletedByShipper(@Param("shipperId") Long shipperId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    @Query("""
        SELECT COUNT(o) FROM Order o
        JOIN ShippingAssignment sa ON sa.orderId = o.id
        WHERE sa.shipperId = :shipperId
        AND o.status = 'SHIPPING'
        AND o.createdAt BETWEEN :from AND :to
    """)
    long countInProgressByShipper(@Param("shipperId") Long shipperId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(o.shippingFee), 0) FROM Order o
        JOIN ShippingAssignment sa ON sa.orderId = o.id
        WHERE sa.shipperId = :shipperId
        AND o.status = 'COMPLETED'
        AND o.createdAt BETWEEN :from AND :to
    """)
    BigDecimal sumShippingFeeByShipper(@Param("shipperId") Long shipperId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    @Query("""
        SELECT new com.alotra.dto.ShipperRevenueDTO(
            CAST(o.createdAt AS LocalDate),
            SUM(o.shippingFee)
        )
        FROM Order o
        JOIN ShippingAssignment sa ON sa.orderId = o.id
        WHERE sa.shipperId = :shipperId
        AND o.status = 'COMPLETED'
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY CAST(o.createdAt AS LocalDate)
        ORDER BY CAST(o.createdAt AS LocalDate)
    """)
    List<ShipperRevenueDTO> getDailyRevenueForShipper(
            @Param("shipperId") Long shipperId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
        SELECT new com.alotra.dto.ShipperStatusCountDTO(
            o.status,
            COUNT(o)
        )
        FROM Order o
        JOIN ShippingAssignment sa ON sa.orderId = o.id
        WHERE sa.shipperId = :shipperId
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY o.status
    """)
    List<ShipperStatusCountDTO> getStatusCountForShipper(
            @Param("shipperId") Long shipperId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
    	    SELECT new com.alotra.dto.ShipperHistoryDTO(
    	        o.code,
    	        o.createdAt,
    	        o.shippingFee,
    	        o.shippingFee * 0.7
    	    )
    	    FROM Order o
    	    JOIN ShippingAssignment sa ON sa.orderId = o.id
    	    WHERE sa.shipperId = :shipperId
    	    AND o.status = 'COMPLETED'
    	    AND o.createdAt BETWEEN :from AND :to
    	    ORDER BY o.createdAt DESC
    	""")
    	List<ShipperHistoryDTO> getHistoryForShipper(
    	        @Param("shipperId") Long shipperId,
    	        @Param("from") LocalDateTime from,
    	        @Param("to") LocalDateTime to
    	);


    @Query(value = """
    	    SELECT u.Id, u.FullName, COUNT(o.Id) AS totalOrders, SUM(o.Total) AS totalSpent
    	    FROM Orders o
    	    JOIN Users u ON u.Id = o.UserId
    	    WHERE o.BranchId = :branchId
    	      AND o.Status = 'COMPLETED'
    	      AND o.CreatedAt BETWEEN :from AND :to
    	    GROUP BY u.Id, u.FullName
    	    ORDER BY totalSpent DESC
    	""", nativeQuery = true)
    	List<Object[]> findTopCustomersByBranchAndDateRange(
    	        @Param("branchId") Long branchId,
    	        @Param("from") LocalDateTime from,
    	        @Param("to") LocalDateTime to
    	);





    	@Query("""
    		    SELECT CAST(o.createdAt AS date) AS date,
    		           COUNT(o.id) AS totalOrders,
    		           SUM(o.total) AS totalRevenue
    		    FROM Order o
    		    WHERE o.branchId = :branchId
    		      AND o.status = 'COMPLETED'
    		      AND o.createdAt BETWEEN :from AND :to
    		    GROUP BY CAST(o.createdAt AS date)
    		    ORDER BY date
    		""")
    		List<Object[]> findRevenueStatisticsByBranchAndDateRange(
    		        @Param("branchId") Long branchId,
    		        @Param("from") LocalDateTime from,
    		        @Param("to") LocalDateTime to
    		);

    // ==========================
    // 🔍 TÌM KIẾM ĐƠN HÀNG (VENDOR)
    // ==========================
    @Query(value = """
        SELECT o.* FROM Orders o
        JOIN Branches b ON o.BranchId = b.Id
        WHERE b.ManagerId = :vendorId
        AND (:status IS NULL OR o.Status = :status)
        AND (:from IS NULL OR o.CreatedAt >= :from)
        AND (:to IS NULL OR o.CreatedAt <= :to)
        AND (:q IS NULL OR o.Code LIKE CONCAT('%', :q, '%'))
        ORDER BY o.CreatedAt DESC
    """, nativeQuery = true)
    List<Order> searchVendorOrders(
            @Param("vendorId") Long vendorId,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("q") String keyword
    );
}