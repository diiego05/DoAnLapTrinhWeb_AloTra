package com.alotra.service;

import com.alotra.dto.dashboard.*;
import com.alotra.entity.PromotionalCampaign;
import com.alotra.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BranchRepository branchRepository;
    private final ShipperRepository shipperRepository;
    private final PromotionalCampaignRepository campaignRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 📊 Tổng quan Dashboard
     */
    public DashboardSummaryDTO getSummary() {
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();
        long totalBranches = branchRepository.count();
        long totalShippers = shipperRepository.count();
        BigDecimal totalRevenue = orderRepository.sumTotalCompleted();

        long activePromotions = campaignRepository.countByStatus(
                PromotionalCampaign.CampaignStatus.ACTIVE
        );

        return new DashboardSummaryDTO(
                totalUsers,
                totalOrders,
                totalBranches,
                totalShippers,
                totalRevenue == null ? BigDecimal.ZERO : totalRevenue,
                activePromotions
        );
    }

    /**
     * 📈 Biểu đồ doanh thu theo thời gian
     */
    public List<RevenuePointDTO> getRevenueChart(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);
        return orderRepository.revenueByDateRange(start, end);
    }

    /**
     * 🧾 Biểu đồ số lượng đơn hàng theo trạng thái
     */
    public List<OrderStatusCountDTO> getOrderStatusChart(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);
        return orderRepository.countOrdersByStatus(start, end);
    }

    /**
     * 🏆 Top sản phẩm bán chạy
     */
    public List<TopProductDTO> getTopProducts(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);
        return orderItemRepository.findTopProducts(start, end);
    }

    /**
     * 🏪 Top chi nhánh theo doanh thu
     */
    public List<TopBranchDTO> getTopBranches(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);

        return orderRepository.findTopBranchesNative(start, end)
                .stream()
                .map(row -> new TopBranchDTO(
                        (String) row[0],               // Branch name
                        (BigDecimal) row[1]            // Total revenue
                ))
                .toList();
    }

    /**
     * 🧾 Lấy đơn hàng mới nhất
     */
    public List<LatestOrderDTO> getLatestOrders(int limit) {
        return orderRepository.findLatestOrdersNative(limit)
                .stream()
                .map(row -> new LatestOrderDTO(
                        (String) row[0],                                 // code
                        (String) row[1],                                 // customerName
                        (BigDecimal) row[2],                             // total
                        (String) row[3],                                 // status
                        ((Timestamp) row[4]).toLocalDateTime()           // createdAt
                ))
                .toList();
    }

    public List<PromotionalCampaign> getTopCampaigns(int limit) {
        return campaignRepository.findTopByViewCount(PageRequest.of(0, limit));
    }
}
