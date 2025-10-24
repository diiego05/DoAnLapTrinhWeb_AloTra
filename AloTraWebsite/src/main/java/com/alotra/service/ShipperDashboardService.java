package com.alotra.service;

import com.alotra.dto.dashboard.OrderByDateDTO;
import com.alotra.dto.dashboard.OrderStatusCountDTO;
import com.alotra.dto.dashboard.RecentOrderDTO;
import com.alotra.repository.ShipperDashboardRepository;
import com.alotra.repository.ShipperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipperDashboardService {

    private final ShipperDashboardRepository shipperDashboardRepository;
    private final ShipperRepository shipperRepository;
    private final UserService userService;

    /**
     * ✅ Lấy ID Shipper hiện tại từ User đăng nhập
     */
    private Long getCurrentShipperId() {
        Long userId = userService.getCurrentUserId();
        Long shipperId = shipperRepository.findIdByUserId(userId);
        if (shipperId == null) {
            throw new IllegalStateException("Tài khoản hiện tại không phải shipper hợp lệ hoặc chưa được duyệt");
        }
        return shipperId;
    }

    /**
     * ✅ Đếm đơn hàng theo trạng thái
     */
    public List<OrderStatusCountDTO> getOrderStatusCount() {
        return shipperDashboardRepository.countOrdersByStatus(getCurrentShipperId());
    }

    /**
     * ✅ Đếm đơn giao thành công hôm nay
     */
    public long getTodayDelivered() {
        return shipperDashboardRepository.countTodayDelivered(getCurrentShipperId());
    }

    /**
     * ✅ Đếm đơn đang giao
     */
    public long getInProgress() {
        return shipperDashboardRepository.countInProgress(getCurrentShipperId());
    }

    /**
     * ✅ Tổng đơn đã giao hoàn tất
     */
    public long getTotalDelivered() {
        return shipperDashboardRepository.countTotalDelivered(getCurrentShipperId());
    }

    /**
     * ✅ Lấy danh sách đơn hàng gần đây của shipper
     */
    public List<RecentOrderDTO> getRecentOrders(int limit) {
        return shipperDashboardRepository.findRecentOrders(getCurrentShipperId(), PageRequest.of(0, limit));
    }

    /**
     * ✅ Lấy thống kê đơn theo ngày trong khoảng thời gian
     */
    public List<OrderByDateDTO> getOrdersByDate(String from, String to) {
        Long shipperId = getCurrentShipperId();
        LocalDateTime fromDate = OffsetDateTime.parse(from).toLocalDateTime();
        LocalDateTime toDate = OffsetDateTime.parse(to).toLocalDateTime();
        return shipperDashboardRepository.findOrdersByDate(shipperId, fromDate, toDate);
    }

    /**
     * ✅ Tổng doanh thu = 70% phí vận chuyển của các đơn đã giao
     */
    public BigDecimal getTotalEarnings() {
        return shipperDashboardRepository.sumTotalEarnings(getCurrentShipperId());
    }

    /**
     * ✅ Doanh thu hôm nay = 70% phí vận chuyển của các đơn giao hôm nay
     */
    public BigDecimal getTodayEarnings() {
        return shipperDashboardRepository.sumTodayEarnings(getCurrentShipperId());
    }
}
