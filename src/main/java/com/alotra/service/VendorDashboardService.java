// com/alotra/service/VendorDashboardService.java
package com.alotra.service;

import com.alotra.dto.dashboard.*;
import com.alotra.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorDashboardService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * 📊 Tổng quan thống kê dashboard cho Vendor
     */
    public DashboardSummaryDTO getSummaryForVendor() {
        Long vendorId = userService.getCurrentUserId();
        Long branchId = orderRepository.findBranchIdByVendor(vendorId);

        long totalOrders = orderRepository.countByBranchId(branchId);
        BigDecimal totalRevenue = orderRepository.sumTotalByBranchCompleted(branchId);
        long totalCustomers = userRepository.countDistinctByBranch(branchId);

        return new DashboardSummaryDTO(
                totalCustomers,
                totalOrders,
                1, // Vendor chỉ quản lý 1 chi nhánh
                0, // Không quản lý shipper
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                0 // Không tính chiến dịch tổng quan
        );
    }

    /**
     * 📈 Doanh thu theo ngày (Revenue Chart)
     */
    public List<RevenuePointDTO> getRevenueChart(LocalDate from, LocalDate to) {
        Long vendorId = userService.getCurrentUserId();
        Long branchId = orderRepository.findBranchIdByVendor(vendorId);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);
        return orderRepository.revenueByDateRangeAndBranch(branchId, start, end);
    }

    /**
     * 🧾 Biểu đồ trạng thái đơn hàng
     */
    public List<OrderStatusCountDTO> getOrderStatusChart(LocalDate from, LocalDate to) {
        Long vendorId = userService.getCurrentUserId();
        Long branchId = orderRepository.findBranchIdByVendor(vendorId);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);
        return orderRepository.countOrdersByStatusAndBranch(branchId, start, end);
    }

    /**
     * 🥇 Top sản phẩm bán chạy
     */
    public List<TopProductDTO> getTopProducts(LocalDate from, LocalDate to) {
        Long vendorId = userService.getCurrentUserId();
        Long branchId = orderRepository.findBranchIdByVendor(vendorId);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);
        return orderItemRepository.findTopProductsByBranch(branchId, start, end);
    }

    /**
     * 👤 Top khách hàng thân thiết
     */
    public List<TopCustomerDTO> getTopCustomers(LocalDate from, LocalDate to) {
        Long vendorId = userService.getCurrentUserId();
        Long branchId = orderRepository.findBranchIdByVendor(vendorId);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);
        return orderRepository.findTopCustomersByBranch(branchId, start, end);
    }

    /**
     * 🕒 Đơn hàng mới nhất
     */
    public List<LatestOrderDTO> getLatestOrders(int limit) {
        Long vendorId = userService.getCurrentUserId();
        Long branchId = orderRepository.findBranchIdByVendor(vendorId);

        return orderRepository.findLatestOrdersByBranch(branchId, limit)
                .stream()
                .map(row -> new LatestOrderDTO(
                        (String) row[0],  // code
                        (String) row[1],  // tên khách hàng
                        (BigDecimal) row[2],  // total
                        (String) row[3],  // status
                        ((Timestamp) row[4]).toLocalDateTime() // createdAt
                ))
                .toList();
    }

    /**
     * ⏳ (Tùy chọn) Chuyển ISO UTC (có Z) sang LocalDateTime hệ thống
     * Nếu frontend gửi thời gian dạng 2025-10-21T14:00:00Z
     */
    private LocalDateTime convertUtcToLocal(String isoString) {
        return Instant.parse(isoString)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
