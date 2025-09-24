package com.alotra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shipper") // Tất cả URL sẽ có tiền tố /shipper
public class ShipperController {

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("pageTitle", "Trang Giao Hàng - Bảng điều khiển");
        // Logic cho dashboard của shipper
        return "shipper/dashboard"; // Trỏ đến file /templates/shipper/dashboard.html
    }

    // Thêm các trang khác của shipper ở đây (ví dụ: /shipper/orders/assigned)
}