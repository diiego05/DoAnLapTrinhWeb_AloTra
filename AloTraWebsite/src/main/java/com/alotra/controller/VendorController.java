package com.alotra.controller;

import com.alotra.entity.User;
import com.alotra.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Optional;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("pageTitle", "Kênh Người Bán - Bảng điều khiển");
        return "vendor/dashboard";
    }

    @GetMapping("/orders")
    public String showOrderManagement(Model model) {
        model.addAttribute("pageTitle", "Kênh Người Bán - Quản lý đơn hàng");
        return "vendor/vendor-orders";
    }

    @GetMapping("/products")
    public String showProductManagementPage(Model model) {
        model.addAttribute("pageTitle", "Kênh Người Bán - Quản lý sản phẩm");
        model.addAttribute("currentPage", "products");
        return "vendor/vendor-products";
    }

    @GetMapping("/revenue")
    public String showRevenueManagementPage(Model model) {
        model.addAttribute("pageTitle", "Kênh Người Bán - Quản lý doanh thu");
        model.addAttribute("currentPage", "revenue");
        return "vendor/vendor-revenue";
        // 👉 Tương ứng: /templates/vendor/vendor-revenue.html
    }

}