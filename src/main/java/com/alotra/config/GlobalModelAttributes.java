package com.alotra.config;

import com.alotra.entity.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

        // ✅ Safer authentication checks
        // - avoid treating anonymous authentication as logged-in
        // - avoid calling principal.getClass() when principal is null
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Object principal = auth.getPrincipal();
            if (principal == null) {
                System.out.println("[DEBUG] Authentication principal is null");
            } else {
                System.out.println("[DEBUG] Authentication principal class: " + principal.getClass().getName());

                // Exclude String principal (Spring may set "anonymousUser") and then check for User
                if (!(principal instanceof String) && principal instanceof User user) {
                    userId = user.getId();
                    userName = user.getFullName();
                    isLoggedIn = true;
                    System.out.println("✅ User logged in: " + userName + " (ID: " + userId + ")");
                } else {
                    System.out.println("⚠️ Authenticated but principal is not User instance: " + principal.getClass().getName());
                }
            }
        } else {
            System.out.println("⚠️ User not logged in or anonymous");
        }

        model.addAttribute("currentUserId", userId);
        model.addAttribute("currentUserName", userName);
        model.addAttribute("isLoggedIn", isLoggedIn);
    }
}