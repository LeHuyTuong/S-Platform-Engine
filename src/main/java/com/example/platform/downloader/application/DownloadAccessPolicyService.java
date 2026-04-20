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
            throw new BusinessException("Báº¡n chÆ°a Ä‘Äƒng nháº­p");
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
            throw new AccessDeniedException("KhÃ´ng cÃ³ quyá»n truy cáº­p");
        }
    }

    private void enforceQuota(User user) {
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        int jobsToday = jobRepository.countByUserAndCreatedAtAfter(user, startOfDay);
        int maxJobs = resolveQuota(user);
        if (jobsToday >= maxJobs) {
            throw new BusinessException("ÄÃ£ vÆ°á»£t háº¡n má»©c! Báº¡n chá»‰ cÃ³ thá»ƒ táº£i " + maxJobs + " tá»‡p má»—i ngÃ y.");
        }
    }

    private void enforceProxyPolicy(User user, SubmitSourceRequest request) {
        boolean hasProxy = (request.getProxy() != null && !request.getProxy().isBlank())
                || (request.getProxyRef() != null && !request.getProxyRef().isBlank());
        if ("USER".equals(user.getRole().name()) && hasProxy) {
            throw new AccessDeniedException("Chá»‰ tÃ i khoáº£n PUBLISHER má»›i cÃ³ thá»ƒ dÃ¹ng Proxy riÃªng.");
        }
    }

    private int resolveQuota(User user) {
        return switch (user.getRole()) {
            case ADMIN -> 999;
            case PUBLISHER -> 20;
            default -> 3;
        };
    }
}
