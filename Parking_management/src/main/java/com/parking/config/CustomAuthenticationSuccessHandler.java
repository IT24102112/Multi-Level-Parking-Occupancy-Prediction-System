package com.parking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        String roleParam = request.getParameter("role");
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String targetUrl = "/login?error=Invalid role";
        if (roleParam != null && (roleParam.equals("kmc") || roleParam.equals("it"))) {
            targetUrl = "/admin/login?role=" + roleParam + "&error=Invalid role";
        }

        logger.info("Login attempt with role parameter: {}", roleParam);
        for (GrantedAuthority authority : authorities) {
            logger.info("User has authority: {}", authority.getAuthority());
            String role = authority.getAuthority();
            if ("kmc".equals(roleParam) && role.equals("ROLE_KMC_ADMIN")) {
                targetUrl = "/admin/kmc/reports";
                break;
            } else if ("it".equals(roleParam) && role.equals("ROLE_IT_ADMIN")) {
                targetUrl = "/admin/dashboard";
                break;
            } else if ("user".equals(roleParam) && role.equals("ROLE_USER")) {
                targetUrl = "/user/dashboard";
                break;
            }
        }
        logger.info("Redirecting to: {}", targetUrl);

        if (response.isCommitted()) {
            return;
        }
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}