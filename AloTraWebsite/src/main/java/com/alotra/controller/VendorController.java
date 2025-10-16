package com.alotra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vendor") // Tất cả URL sẽ có tiền tố /vendor
public class VendorController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("pageTitle", "Kênh Người Bán - Bảng điều khiển");
        // Logic cho dashboard của vendor
        return "vendor/dashboard"; // Trỏ đến file /templates/vendor/dashboard.html
    }
    @GetMapping("/orders")
    public String showOrderManagement(Model model) {
        model.addAttribute("pageTitle", "Kênh Người Bán - Quản lý đơn hàng");
        return "vendor/vendor-orders"; // /templates/vendor/vendor-orders.html
    }
    // Thêm các trang khác của vendor ở đây (ví dụ: /vendor/products, /vendor/orders)
}