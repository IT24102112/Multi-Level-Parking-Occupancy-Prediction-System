package com.parking.config;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String role = request.getParameter("role");
        String redirectUrl = "/login?error=true";
        if (role != null && (role.equals("kmc") || role.equals("it"))) {
            redirectUrl = "/admin/login?role=" + role + "&error=true";
        }
        
        if (exception instanceof DisabledException) {
            // User is blacklisted (enabled = 0)
            redirectUrl = "/login?error=blacklisted";
            if (role != null && (role.equals("kmc") || role.equals("it"))) {
                redirectUrl = "/admin/login?role=" + role + "&error=blacklisted";
            }
        }
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
