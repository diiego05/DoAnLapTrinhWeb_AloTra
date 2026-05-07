package com.alotra.controller.api;

import com.alotra.dto.dashboard.*;
import com.alotra.entity.PromotionalCampaign;
import com.alotra.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardApiController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<List<RevenuePointDTO>> getRevenueChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(dashboardService.getRevenueChart(from, to));
    }

    @GetMapping("/order-status-chart")
    public ResponseEntity<List<OrderStatusCountDTO>> getOrderStatusChart(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(dashboardService.getOrderStatusChart(from, to));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductDTO>> getTopProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(dashboardService.getTopProducts(from, to));
    }

    @GetMapping("/top-branches")
    public ResponseEntity<List<TopBranchDTO>> getTopBranches(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(dashboardService.getTopBranches(from, to));
    }

    @GetMapping("/latest-orders")
    public ResponseEntity<List<LatestOrderDTO>> getLatestOrders(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(dashboardService.getLatestOrders(limit));
    }

    @GetMapping("/top-campaigns")
    public List<PromotionalCampaign> getTopCampaigns(@RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getTopCampaigns(limit);
    }
}
