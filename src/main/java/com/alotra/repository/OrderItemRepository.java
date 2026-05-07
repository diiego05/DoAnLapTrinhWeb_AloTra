package com.alotra.repository;

import com.alotra.dto.dashboard.TopProductDTO;
import com.alotra.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 📌 Lấy toàn bộ sản phẩm thuộc một đơn hàng
    List<OrderItem> findByOrderId(Long orderId);

    // 📌 Xoá toàn bộ item khi huỷ đơn hàng (tuỳ chọn)
    void deleteByOrderId(Long orderId);

    // 🏆 Top sản phẩm bán chạy
    @Query("""
        SELECT new com.alotra.dto.dashboard.TopProductDTO(
            oi.productName,
            SUM(oi.quantity),
            SUM(oi.lineTotal)
        )
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status = 'COMPLETED'
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY oi.productName
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<TopProductDTO> findTopProducts(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);


    @Query("""
            SELECT new com.alotra.dto.dashboard.TopProductDTO(
                oi.productName,
                SUM(oi.quantity),
                SUM(oi.lineTotal)
            )
            FROM OrderItem oi
            JOIN oi.order o
            WHERE o.status = 'COMPLETED'
            AND o.branchId = :branchId
            AND o.createdAt BETWEEN :from AND :to
            GROUP BY oi.productName
            ORDER BY SUM(oi.quantity) DESC
        """)
        List<TopProductDTO> findTopProductsByBranch(
                @Param("branchId") Long branchId,
                @Param("from") LocalDateTime from,
                @Param("to") LocalDateTime to
        );

    @Query(value = """
    	    SELECT p.Id, p.Name, SUM(oi.Quantity) AS totalQty, SUM(oi.LineTotal) AS totalRevenue
    	    FROM OrderItems oi
    	    JOIN Orders o ON o.Id = oi.OrderId
    	    JOIN Products p ON p.Id = oi.ProductId
    	    WHERE o.BranchId = :branchId
    	      AND o.Status = 'COMPLETED'
    	      AND o.CreatedAt BETWEEN :from AND :to
    	    GROUP BY p.Id, p.Name
    	    ORDER BY totalQty DESC
    	""", nativeQuery = true)
    	List<Object[]> findTopProductsByBranchAndDateRange(
    	        @Param("branchId") Long branchId,
    	        @Param("from") LocalDateTime from,
    	        @Param("to") LocalDateTime to
    	);

}
