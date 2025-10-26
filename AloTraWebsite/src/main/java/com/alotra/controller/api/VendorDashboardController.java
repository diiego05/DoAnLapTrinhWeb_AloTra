// com/alotra/controller/api/VendorDashboardController.java
package com.alotra.controller.api;

import com.alotra.dto.dashboard.*;
import com.alotra.service.VendorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vendor/dashboard")
@RequiredArgsConstructor
public class VendorDashboardController {

    private final VendorDashboardService vendorDashboardService;

    @GetMapping("/summary")
    public DashboardSummaryDTO getSummary() {
        return vendorDashboardService.getSummaryForVendor();
    }

    @GetMapping("/revenue")
    public List<RevenuePointDTO> getRevenueChart(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return vendorDashboardService.getRevenueChart(from, to);
    }

    @GetMapping("/order-status")
    public List<OrderStatusCountDTO> getOrderStatusChart(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return vendorDashboardService.getOrderStatusChart(from, to);
    }

    @GetMapping("/top-products")
    public List<TopProductDTO> getTopProducts(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return vendorDashboardService.getTopProducts(from, to);
    }

    @GetMapping("/top-customers")
    public List<TopCustomerDTO> getTopCustomers(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return vendorDashboardService.getTopCustomers(from, to);
    }

    @GetMapping("/latest-orders")
    public List<LatestOrderDTO> getLatestOrders(@RequestParam(defaultValue = "5") int limit) {
        return vendorDashboardService.getLatestOrders(limit);
    }
}
