package com.example.platform.downloader.ui;

import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.application.JobManager;
import com.example.platform.downloader.application.DownloaderService;
import com.example.platform.kernel.exception.BusinessException;
import com.example.platform.kernel.exception.ResourceNotFoundException;
import com.example.platform.kernel.ui.RestResponse;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.example.platform.modules.user.domain.User;
import com.example.platform.modules.user.infrastructure.UserRepository;

@Controller
@RequestMapping("/downloader")
public class DownloadController {

    private final DownloaderService downloaderService;
    private final JobManager jobManager;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public DownloadController(DownloaderService downloaderService,
                              JobManager jobManager,
                              JobRepository jobRepository,
                              UserRepository userRepository) {
        this.downloaderService = downloaderService;
        this.jobManager = jobManager;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pages
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model, Principal principal) {
        if (principal != null) {
            userRepository.findByEmail(principal.getName()).ifPresentOrElse(user -> {
                model.addAttribute("jobs", jobRepository.findByUserOrderByCreatedAtDesc(user));
                model.addAttribute("currentUser", user);
                model.addAttribute("hasCookies", downloaderService.hasCookieFile(user));
            }, () -> {
                model.addAttribute("jobs", Collections.emptyList());
                model.addAttribute("hasCookies", false);
            });
        } else {
            model.addAttribute("jobs", Collections.emptyList());
            model.addAttribute("hasCookies", false);
        }
        return "downloader";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Download submission
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/submit")
    @ResponseBody
    public RestResponse<Job> submit(@RequestBody Map<String, String> payload,
                                     Principal principal, HttpSession session) {
        if (principal == null) {
            throw new BusinessException("Bạn chưa đăng nhập");
        }
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Bug 4 Fix: serialize quota check + submit per-user để tránh concurrent bypass
        synchronized (jobManager.getUserLock(user.getId().toString())) {

            LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
            int jobsToday = jobRepository.countByUserAndCreatedAtAfter(user, startOfDay);

            int maxJobs = resolveQuota(user);
            if (jobsToday >= maxJobs) {
                throw new BusinessException("Đã vượt hạn mức! Bạn chỉ có thể tải " + maxJobs + " tệp mỗi ngày.");
            }

            String url = payload.get("url");
            if (url == null || url.isBlank()) {
                throw new BusinessException("Vui lòng nhập URL");
            }

            // Block proxy usage for normal users
            if ("USER".equals(user.getRole().name())
                    && payload.containsKey("proxy")
                    && !payload.get("proxy").isBlank()) {
                throw new org.springframework.security.access.AccessDeniedException("Chỉ tài khoản PUBLISHER mới có thể dùng Proxy riêng.");
            }

            Job job = downloaderService.submitDownload(payload, user, session);
            return RestResponse.ok(job, "Job submitted successfully");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Job status
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/status/{id}")
    @ResponseBody
    public RestResponse<Job> getStatus(@PathVariable String id, Principal principal) {
        Job job = jobManager.getJob(id);
        if (job == null) {
            throw new ResourceNotFoundException("Job not found: " + id);
        }
        // Chỉ cho phép xem job của chính mình (hoặc ADMIN)
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user != null && !isOwnerOrAdmin(job, user)) {
                throw new org.springframework.security.access.AccessDeniedException("Không có quyền truy cập");
            }
        }
        return RestResponse.ok(job);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File serving (Feature 1)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Liệt kê các file đã tải về trong một job.
     * Chỉ owner hoặc ADMIN mới được xem.
     */
    @GetMapping("/api/files/{jobId}")
    @ResponseBody
    public RestResponse<List<Map<String, String>>> listFiles(@PathVariable String jobId, Principal principal) {
        Job job = jobManager.getJob(jobId);
        if (job == null) throw new ResourceNotFoundException("Job not found");

        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user == null || !isOwnerOrAdmin(job, user)) {
                throw new org.springframework.security.access.AccessDeniedException("Không có quyền truy cập");
            }
        } else {
            throw new BusinessException("Bạn chưa đăng nhập");
        }

        List<Map<String, String>> files = downloaderService.listJobFiles(jobId);
        return RestResponse.ok(files);
    }

    /**
     * Serve file về client dưới dạng attachment download.
     * Chỉ owner hoặc ADMIN mới được tải.
     * URL pattern: GET /downloader/files/{jobId}/{filename}
     */
    @GetMapping("/files/{jobId}/{filename}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String jobId,
            @PathVariable String filename,
            Principal principal) throws IOException {

        Job job = jobManager.getJob(jobId);
        if (job == null) return ResponseEntity.notFound().build();

        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user == null || !isOwnerOrAdmin(job, user)) {
                return ResponseEntity.status(403).build();
            }
        } else {
            return ResponseEntity.status(401).build();
        }

        ResponseEntity<Resource> response = downloaderService.serveFile(jobId, filename);
        return response != null ? response : ResponseEntity.notFound().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cookie management (Feature 2: per-user isolation)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/upload-cookies")
    @ResponseBody
    public RestResponse<Void> uploadCookies(
            @RequestParam("file") MultipartFile file, Principal principal) {
        if (principal == null) {
            throw new BusinessException("Bạn chưa đăng nhập");
        }
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        try {
            downloaderService.saveCookieFile(file, user);
            return RestResponse.ok(null, "Tải lên cookie thành công");
        } catch (Exception e) {
            throw new BusinessException("Lỗi upload: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/cookies")
    @ResponseBody
    public RestResponse<Void> deleteCookies(Principal principal) {
        if (principal == null) {
            throw new BusinessException("Bạn chưa đăng nhập");
        }
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        try {
            downloaderService.deleteCookieFile(user);
            return RestResponse.ok(null, "Đã xoá file cookie");
        } catch (Exception e) {
            throw new BusinessException("Lỗi khi xóa: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Telegram Chat ID management (Feature 3)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * User tự cập nhật Telegram Chat ID của họ.
     * Lấy chat_id: nhắn /start cho @userinfobot trên Telegram.
     *
     * POST /downloader/api/telegram-chatid
     * Body: { "chatId": "123456789" }
     */
    @PostMapping("/api/telegram-chatid")
    @ResponseBody
    public RestResponse<Void> updateTelegramChatId(
            @RequestBody Map<String, String> body, Principal principal) {
        if (principal == null) {
            throw new BusinessException("Bạn chưa đăng nhập");
        }
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String chatId = body.getOrDefault("chatId", "").trim();
        // Cho phép xóa chat_id bằng cách gửi chuỗi rỗng
        user.setTelegramChatId(chatId.isEmpty() ? null : chatId);
        userRepository.save(user);

        String msg = chatId.isEmpty() ? "Đã xóa Telegram Chat ID" : "Đã lưu Telegram Chat ID: " + chatId;
        return RestResponse.ok(null, msg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int resolveQuota(User user) {
        return switch (user.getRole()) {
            case ADMIN -> 999;
            case PUBLISHER -> 20;
            default -> 3;
        };
    }

    private boolean isOwnerOrAdmin(Job job, User user) {
        if ("ADMIN".equals(user.getRole().name())) return true;
        return job.getUser() != null && job.getUser().getId().equals(user.getId());
    }
}
