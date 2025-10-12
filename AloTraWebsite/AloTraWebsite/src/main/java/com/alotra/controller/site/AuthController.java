package com.alotra.controller.site;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller("siteAuthController")
public class AuthController {

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register"; // Trả về file register.html
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login"; // Trả về file login.html
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }
}