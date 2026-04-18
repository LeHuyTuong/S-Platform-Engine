package com.example.platform.downloader;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.example.platform.modules.user.domain.User;
import com.example.platform.modules.user.infrastructure.UserRepository;

@Controller
@RequestMapping("/downloader")
public class DownloadController {

    private final DownloaderService downloaderService;
    private final JobManager jobManager;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public DownloadController(DownloaderService downloaderService, JobManager jobManager, JobRepository jobRepository, UserRepository userRepository) {
        this.downloaderService = downloaderService;
        this.jobManager = jobManager;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }


    @GetMapping
    public String index(Model model, Principal principal) {
        if (principal != null) {
            // Người dùng đã đăng nhập - hiển thị job của họ
            java.util.Optional<User> userOpt = userRepository.findByEmail(principal.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("jobs", jobRepository.findByUserOrderByCreatedAtDesc(user));
                model.addAttribute("currentUser", user);
            } else {
                model.addAttribute("jobs", java.util.Collections.emptyList());
            }
        } else {
            // Khách - hiển thị danh sách trống
            model.addAttribute("jobs", java.util.Collections.emptyList());
        }
        model.addAttribute("hasCookies", downloaderService.hasCookieFile());
        return "downloader";
    }

    @PostMapping("/api/submit")
    @ResponseBody
    public ResponseEntity<?> submit(@RequestBody Map<String, String> payload, Principal principal) {
        // Find current user
        if (principal == null) {
             return ResponseEntity.status(401).body(Map.of("message", "Bạn chưa đăng nhập"));
        }
        User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));

        // Implement Quota Check
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        int jobsToday = jobRepository.countByUserAndCreatedAtAfter(user, startOfDay);
        
        int maxJobs = 3; // Default USER
        if ("PUBLISHER".equals(user.getRole().name())) {
            maxJobs = 20;
        } else if ("ADMIN".equals(user.getRole().name())) {
            maxJobs = 999;
        }

        if (jobsToday >= maxJobs) {
            return ResponseEntity.status(429).body(Map.of("message", "Đã vượt hạn mức! Bạn chỉ có thể tải " + maxJobs + " tệp mỗi ngày."));
        }

        // Validate payload
        String url = payload.get("url");
        if (url == null || url.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập URL"));
        }
        
        // Block proxy usage for normal users
        if ("USER".equals(user.getRole().name()) && payload.containsKey("proxy") && !payload.get("proxy").isEmpty()) {
             return ResponseEntity.status(403).body(Map.of("message", "Chỉ tài khoản PUBLISHER mới có thể dùng Proxy riêng."));
        }

        Job job = downloaderService.submitDownload(payload, user);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/api/status/{id}")
    @ResponseBody
    public Job getStatus(@PathVariable String id) {
        return jobManager.getJob(id);
    }

    @PostMapping("/api/upload-cookies")
    @ResponseBody
    public Map<String, String> uploadCookies(@RequestParam("file") MultipartFile file) {
        try {
            downloaderService.saveCookieFile(file);
            return Map.of("status", "success", "message", "Tải lên cookie thành công");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @DeleteMapping("/api/cookies")
    @ResponseBody
    public Map<String, String> deleteCookies() {
        try {
            downloaderService.deleteCookieFile();
            return Map.of("status", "success", "message", "Đã xoá file cookie");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
