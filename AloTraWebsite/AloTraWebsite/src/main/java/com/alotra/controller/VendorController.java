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

    // Quản lý sản phẩm yêu thích
    @GetMapping("/favorites")
    public String showFavorites(Model model) {
        model.addAttribute("pageTitle", "Kênh Người Bán - Sản phẩm yêu thích");
        // Logic lấy danh sách sản phẩm yêu thích của vendor
        return "vendor/favorites"; // Trỏ đến file /templates/vendor/favorites.html
    }

    // Quản lý khuyến mãi
    @GetMapping("/promotions")
    public String showPromotions(Model model) {
        model.addAttribute("pageTitle", "Kênh Người Bán - Khuyến mãi");
        // Logic lấy danh sách khuyến mãi của vendor
        return "vendor/promotions"; // Trỏ đến file /templates/vendor/promotions.html
    }

    // Quản lý mã giảm giá
    @GetMapping("/coupons")
    public String showCoupons(Model model) {
        model.addAttribute("pageTitle", "Kênh Người Bán - Mã giảm giá");
        // Logic lấy danh sách mã giảm giá của vendor
        return "vendor/coupons"; // Trỏ đến file /templates/vendor/coupons.html
    }

    // Thêm các trang khác của vendor ở đây (ví dụ: /vendor/products, /vendor/orders)
}