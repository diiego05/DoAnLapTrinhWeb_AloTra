package com.alotra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/account") // Tất cả URL sẽ có tiền tố /account
public class AccountController {

    @GetMapping("/profile")
    public String showProfilePage(Model model) {
        model.addAttribute("pageTitle", "Thông Tin Tài Khoản");
        // Logic để lấy thông tin user đang đăng nhập
        return "account/profile"; // Trỏ đến file /templates/account/profile.html
    }

    @GetMapping("/orders")
    public String showOrdersPage(Model model) {
        model.addAttribute("pageTitle", "Lịch Sử Đơn Hàng");
        // Logic để lấy lịch sử đơn hàng của user
        return "account/orders"; // Trỏ đến file /templates/account/orders.html
    }
}