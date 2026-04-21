package com.example.platform.modules.user.ui;

import com.example.platform.downloader.application.DownloadAccessPolicyService;
import com.example.platform.kernel.ui.RestResponse;
import com.example.platform.modules.user.domain.User;
import com.example.platform.modules.user.infrastructure.UserRepository;
import com.example.platform.modules.user.ui.dto.AuthSessionResponse;
import com.example.platform.modules.user.ui.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final UserRepository userRepository;
    private final DownloadAccessPolicyService accessPolicyService;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          SessionAuthenticationStrategy sessionAuthenticationStrategy,
                          UserRepository userRepository,
                          DownloadAccessPolicyService accessPolicyService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.userRepository = userRepository;
        this.accessPolicyService = accessPolicyService;
    }

    @PostMapping("/login")
    public RestResponse<AuthSessionResponse> login(@Valid @RequestBody LoginRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return RestResponse.ok(toSessionResponse(user), "Login successful");
    }

    @PostMapping("/logout")
    public RestResponse<Void> logout(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        SecurityContextHolder.clearContext();
        return RestResponse.ok(null, "Logout successful");
    }

    @GetMapping("/csrf")
    public RestResponse<Map<String, String>> csrf(CsrfToken csrfToken) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("token", csrfToken.getToken());
        payload.put("headerName", csrfToken.getHeaderName());
        payload.put("parameterName", csrfToken.getParameterName());
        return RestResponse.ok(payload);
    }

    @GetMapping("/me")
    public RestResponse<AuthSessionResponse> me(Principal principal) {
        if (principal == null) {
            AuthSessionResponse anonymous = new AuthSessionResponse();
            anonymous.setAuthenticated(false);
            return RestResponse.ok(anonymous);
        }
        User user = accessPolicyService.currentUser(principal);
        return RestResponse.ok(toSessionResponse(user));
    }

    private AuthSessionResponse toSessionResponse(User user) {
        AuthSessionResponse response = new AuthSessionResponse();
        response.setAuthenticated(true);
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setEnabled(user.isEnabled());
        response.setDailyQuota(accessPolicyService.resolveQuota(user));
        response.setJobsToday(accessPolicyService.jobsToday(user));
        response.setCanUseProxy(accessPolicyService.canUseProxy(user));
        response.setCanManageRuntimeSettings(accessPolicyService.canManageRuntimeSettings(user));
        return response;
    }
}
