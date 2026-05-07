package com.alotra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shipper") // 👉 Tất cả URL của shipper sẽ có tiền tố /shipper
public class ShipperController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("pageTitle", "Kênh Shipper - Bảng điều khiển");
        return "shipper/dashboard";
        // Tương ứng: /templates/shipper/dashboard.html
    }

    @GetMapping("/deliveries")
    public String showDeliveryManagement(Model model) {
        model.addAttribute("pageTitle", "Kênh Shipper - Quản lý giao hàng");
        return "shipper/shipper-deliveries";
        // Tương ứng: /templates/shipper/shipper-deliveries.html
    }

    @GetMapping("/statistics")
    public String showStatistics(Model model) {
        model.addAttribute("pageTitle", "Kênh Shipper - Thống kê giao hàng");
        model.addAttribute("currentPage", "statistics");
        return "shipper/shipper-statistics";
        // Tương ứng: /templates/shipper/shipper-statistics.html
    }
}
