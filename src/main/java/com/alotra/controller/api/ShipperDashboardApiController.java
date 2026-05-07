package com.alotra.controller.api;

import com.alotra.dto.dashboard.OrderByDateDTO;
import com.alotra.dto.dashboard.OrderStatusCountDTO;
import com.alotra.dto.dashboard.RecentOrderDTO;
import com.alotra.service.ShipperDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipper/dashboard")
@RequiredArgsConstructor
public class ShipperDashboardApiController {

    private final ShipperDashboardService shipperDashboardService;

    /**
     * ✅ Tổng hợp thống kê dashboard của shipper
     */
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        Map<String, Object> map = new HashMap<>();
        map.put("todayOrders", shipperDashboardService.getTodayDelivered());
        map.put("inProgressOrders", shipperDashboardService.getInProgress());
        map.put("completedOrders", shipperDashboardService.getTotalDelivered());
        map.put("totalEarnings", shipperDashboardService.getTotalEarnings());
        map.put("todayEarnings", shipperDashboardService.getTodayEarnings());
        return map;
    }

    /**
     * ✅ Đếm đơn theo trạng thái
     */
    @GetMapping("/order-status")
    public List<OrderStatusCountDTO> getOrderStatus() {
        return shipperDashboardService.getOrderStatusCount();
    }

    /**
     * ✅ Lấy danh sách đơn hàng gần đây
     */
    @GetMapping("/recent-orders")
    public List<RecentOrderDTO> getRecentOrders(
            @RequestParam(defaultValue = "5") int limit) {
        return shipperDashboardService.getRecentOrders(limit);
    }

    /**
     * ✅ Lấy thống kê đơn theo ngày (để vẽ biểu đồ doanh thu / đơn)
     */
    @GetMapping("/orders-by-date")
    public List<OrderByDateDTO> getOrdersByDate(
            @RequestParam String from,
            @RequestParam String to) {
        return shipperDashboardService.getOrdersByDate(from, to);
    }
}
