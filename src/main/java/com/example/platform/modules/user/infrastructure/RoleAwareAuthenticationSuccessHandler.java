package com.example.platform.modules.user.infrastructure;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class RoleAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final String adminUiUrl;
    private final String downloaderUiUrl;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public RoleAwareAuthenticationSuccessHandler(String adminUiUrl, String downloaderUiUrl) {
        this.adminUiUrl = adminUiUrl;
        this.downloaderUiUrl = downloaderUiUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        redirectStrategy.sendRedirect(request, response, determineTargetUrl(authentication));
    }

    private String determineTargetUrl(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (isAdmin) {
            if (StringUtils.hasText(adminUiUrl)) {
                return adminUiUrl;
            }
            return "/admin";
        }

        if (StringUtils.hasText(downloaderUiUrl)) {
            return downloaderUiUrl;
        }

        return "/downloader";
    }
}
