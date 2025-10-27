package com.alotra.config;

import com.alotra.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Long userId = null;
        String userName = null;
        boolean isLoggedIn = false;

        // ✅ KIỂM TRA KỸ CÀNG
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            Object principal = auth.getPrincipal();
            // Debug: in case principal isn't our entity, print its class for investigation
            System.out.println("[DEBUG] Authentication principal class: " + principal.getClass().getName());

            if (principal instanceof User user) {
                userId = user.getId();
                userName = user.getFullName();
                isLoggedIn = true;
                System.out.println("✅ User logged in: " + userName + " (ID: " + userId + ")");
            } else {
                System.out.println("⚠️ Authenticated but principal is not User instance: " + principal.getClass().getName());
            }
        } else {
            System.out.println("⚠️ User not logged in (anonymousUser or null)");
        }

        model.addAttribute("currentUserId", userId);
        model.addAttribute("currentUserName", userName);
        model.addAttribute("isLoggedIn", isLoggedIn);
    }
}