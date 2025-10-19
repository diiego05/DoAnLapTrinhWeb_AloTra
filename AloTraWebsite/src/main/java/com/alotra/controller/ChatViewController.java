package com.alotra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/chat")
public class ChatViewController {

    /**
     * Trang chat cho customer
     * GET /chat/customer
     */
    @GetMapping("/customer")
    public String customerChat() {
        return "admin/customer-chat";
    }

    /**
     * Trang admin dashboard
     * GET /chat/admin
     */
    @GetMapping("/admin")
    public String adminDashboard() {
        return "admin/admin-chat-dashboard";
    }
}