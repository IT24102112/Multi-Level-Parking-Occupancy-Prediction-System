package com.parking.config;

import com.parking.controller.AdminSelectionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminLoginTest {

    // ---------- Controller tests (standalone MockMvc) ----------
    private MockMvc mockMvc;

    @InjectMocks
    private AdminSelectionController adminSelectionController;

    // ---------- Success handler tests ----------
    @InjectMocks
    private CustomAuthenticationSuccessHandler successHandler;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RedirectStrategy redirectStrategy;

    @BeforeEach
    void setUp() {
        // Standalone setup for controller
        mockMvc = MockMvcBuilders.standaloneSetup(adminSelectionController).build();
        // Inject the mock redirect strategy into the success handler
        successHandler.setRedirectStrategy(redirectStrategy);
    }

    // ==================== 1. AdminSelectionController tests ====================
    @Test
    void selectAdminPage_ReturnsSelectView() throws Exception {
        mockMvc.perform(get("/admin/select"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/select"));
    }

    @Test
    void loginPage_WithRoleParameter_ReturnsLoginView() throws Exception {
        mockMvc.perform(get("/admin/login").param("role", "it"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"))
                .andExpect(model().attribute("selectedRole", "it"));
    }

    @Test
    void loginPage_WithKmcRole_ReturnsLoginView() throws Exception {
        mockMvc.perform(get("/admin/login").param("role", "kmc"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"))
                .andExpect(model().attribute("selectedRole", "kmc"));
    }

    // ==================== 2. CustomAuthenticationSuccessHandler tests ====================
    @SuppressWarnings("unchecked")
    private void mockAuthorities(Collection<? extends GrantedAuthority> authorities) {
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
    }

    @Test
    void successHandler_ITAdmin_RedirectsToDashboard() throws Exception {
        mockAuthorities(List.of(new SimpleGrantedAuthority("ROLE_IT_ADMIN")));
        when(request.getParameter("role")).thenReturn("it");

        successHandler.handle(request, response, authentication);

        verify(redirectStrategy).sendRedirect(request, response, "/admin/dashboard");
    }

    @Test
    void successHandler_KMCAdmin_RedirectsToReports() throws Exception {
        mockAuthorities(List.of(new SimpleGrantedAuthority("ROLE_KMC_ADMIN")));
        when(request.getParameter("role")).thenReturn("kmc");

        successHandler.handle(request, response, authentication);

        verify(redirectStrategy).sendRedirect(request, response, "/admin/kmc/reports");
    }

    @Test
    void successHandler_UserRole_RedirectsToUserDashboard() throws Exception {
        mockAuthorities(List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(request.getParameter("role")).thenReturn("user");

        successHandler.handle(request, response, authentication);

        verify(redirectStrategy).sendRedirect(request, response, "/user/dashboard");
    }

    @Test
    void successHandler_RoleMismatch_RedirectsToSelectWithError() throws Exception {
        mockAuthorities(List.of(new SimpleGrantedAuthority("ROLE_IT_ADMIN")));
        when(request.getParameter("role")).thenReturn("kmc");

        successHandler.handle(request, response, authentication);

        verify(redirectStrategy).sendRedirect(request, response, "/admin/select?error=Invalid role");
    }

    @Test
    void successHandler_NoRoleParameter_RedirectsToSelectWithError() throws Exception {
        mockAuthorities(List.of(new SimpleGrantedAuthority("ROLE_IT_ADMIN")));
        when(request.getParameter("role")).thenReturn(null);

        successHandler.handle(request, response, authentication);

        verify(redirectStrategy).sendRedirect(request, response, "/admin/select?error=Invalid role");
    }

    @Test
    void successHandler_UnknownRole_RedirectsToSelectWithError() throws Exception {
        mockAuthorities(List.of(new SimpleGrantedAuthority("ROLE_IT_ADMIN")));
        when(request.getParameter("role")).thenReturn("unknown");

        successHandler.handle(request, response, authentication);

        verify(redirectStrategy).sendRedirect(request, response, "/admin/select?error=Invalid role");
    }
}