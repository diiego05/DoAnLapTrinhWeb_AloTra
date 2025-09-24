package com.alotra.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(auth.getAuthority())) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return;
            } else if ("ROLE_VENDOR".equals(auth.getAuthority())) {
                response.sendRedirect(request.getContextPath() + "/vendor/dashboard");
                return;
            } else if ("ROLE_SHIPPER".equals(auth.getAuthority())) {
                response.sendRedirect(request.getContextPath() + "/shipper/dashboard");
                return;
            }
        }
        // Mặc định cho các vai trò khác (ví dụ: USER)
        response.sendRedirect(request.getContextPath() + "/");
    }
}
