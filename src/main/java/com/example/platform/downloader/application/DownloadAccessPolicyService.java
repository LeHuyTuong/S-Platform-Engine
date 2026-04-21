package com.example.platform.downloader.application;

import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.ui.dto.SubmitSourceRequest;
import com.example.platform.kernel.exception.BusinessException;
import com.example.platform.kernel.exception.ResourceNotFoundException;
import com.example.platform.modules.user.domain.User;
import com.example.platform.modules.user.infrastructure.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class DownloadAccessPolicyService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    public DownloadAccessPolicyService(UserRepository userRepository, JobRepository jobRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
    }

    public User currentUser(Principal principal) {
        if (principal == null) {
            throw new BusinessException("Ban chua dang nhap");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public void enforceSubmissionPolicy(User user, SubmitSourceRequest request) {
        enforceProxyPolicy(user, request);
        enforceQuota(user);
    }

    public void ensureOwnerOrAdmin(User owner, User actor) {
        if ("ADMIN".equals(actor.getRole().name())) {
            return;
        }
        if (owner == null || !owner.getId().equals(actor.getId())) {
            throw new AccessDeniedException("Khong co quyen truy cap");
        }
    }

    public int resolveQuota(User user) {
        return switch (user.getRole()) {
            case ADMIN -> 999;
            case PUBLISHER -> 20;
            default -> 3;
        };
    }

    public int jobsToday(User user) {
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return jobRepository.countByUserAndCreatedAtAfter(user, startOfDay);
    }

    public boolean canUseProxy(User user) {
        return !"USER".equals(user.getRole().name());
    }

    public boolean canManageRuntimeSettings(User user) {
        String role = user.getRole().name();
        return "ADMIN".equals(role) || "PUBLISHER".equals(role);
    }

    private void enforceQuota(User user) {
        int jobsToday = jobsToday(user);
        int maxJobs = resolveQuota(user);
        if (jobsToday >= maxJobs) {
            throw new BusinessException("Da vuot han muc! Ban chi co the tai " + maxJobs + " tep moi ngay.");
        }
    }

    private void enforceProxyPolicy(User user, SubmitSourceRequest request) {
        boolean hasProxy = (request.getProxy() != null && !request.getProxy().isBlank())
                || (request.getProxyRef() != null && !request.getProxyRef().isBlank());
        if (hasProxy && !canUseProxy(user)) {
            throw new AccessDeniedException("Chi tai khoan PUBLISHER moi co the dung proxy rieng.");
        }
    }
}
