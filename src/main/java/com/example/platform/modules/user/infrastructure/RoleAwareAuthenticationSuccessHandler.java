package com.example.platform.modules.user.infrastructure;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class RoleAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(RoleAwareAuthenticationSuccessHandler.class);
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
        String targetUrl = determineTargetUrl(authentication);
        log.info("[Auth] Success! Redirecting user={} to {}", authentication.getName(), targetUrl);
        redirectStrategy.sendRedirect(request, response, targetUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (isAdmin) {
            if (StringUtils.hasText(adminUiUrl)) {
                return adminUiUrl;
            }
            return "/app/admin";
        }

        if (StringUtils.hasText(downloaderUiUrl)) {
            return downloaderUiUrl;
        }

        return "/app/downloader";
    }
}
