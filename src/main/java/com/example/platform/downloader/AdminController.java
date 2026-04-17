package com.example.platform.downloader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.platform.modules.user.infrastructure.UserRepository;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final JobManager jobManager;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AppSettings appSettings;
    private final String downloadDir;

    public AdminController(JobManager jobManager,
                           JobRepository jobRepository,
                           UserRepository userRepository,
                           AppSettings appSettings,
                           @Value("${app.downloader.output-dir:downloads}") String downloadDir) {
        this.jobManager = jobManager;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.appSettings = appSettings;
        this.downloadDir = downloadDir;
    }

    // Trang Admin Dashboard chính
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("jobs", jobRepository.findAll().stream()
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .collect(Collectors.toList()));
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("settings", appSettings);
        model.addAttribute("diskUsage", getDiskUsageMb());
        return "admin";
    }

    // API: Xoá file tải của một Job cụ thể
    @DeleteMapping("/api/job/{id}/clear")
    @ResponseBody
    public ResponseEntity<?> clearJobFiles(@PathVariable String id) {
        Job job = jobRepository.findById(id).orElse(null);
        if (job == null) return ResponseEntity.notFound().build();

        try {
            // Tìm và xoá file liên quan đến job (search theo ID trong tên file)
            Path dir = Paths.get(downloadDir);
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().contains("[" + id.substring(0, 8) + "]"))
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
            }
            return ResponseEntity.ok(Map.of("message", "Files cleared for job " + id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    // API: Xoá tất cả file tạm trong thư mục downloads
    @DeleteMapping("/api/storage/clear")
    @ResponseBody
    public ResponseEntity<?> clearAllStorage() {
        try {
            Path dir = Paths.get(downloadDir);
            long deleted = 0;
            if (Files.exists(dir)) {
                List<Path> files = Files.walk(dir)
                        .filter(Files::isRegularFile)
                        .filter(p -> !p.getFileName().toString().equals("cookies.txt")
                                  && !p.getFileName().toString().equals("downloaded.txt"))
                        .collect(Collectors.toList());
                for (Path p : files) {
                    Files.deleteIfExists(p);
                    deleted++;
                }
            }
            return ResponseEntity.ok(Map.of("message", "Cleared " + deleted + " files from storage."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    // API: Cập nhật cấu hình hệ thống (nóng - không cần restart)
    @PostMapping("/api/settings")
    @ResponseBody
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, String> body) {
        try {
            if (body.containsKey("sleepInterval"))
                appSettings.setSleepInterval(Integer.parseInt(body.get("sleepInterval")));
            if (body.containsKey("concurrentFragments"))
                appSettings.setConcurrentFragments(Integer.parseInt(body.get("concurrentFragments")));
            if (body.containsKey("sleepRequests"))
                appSettings.setSleepRequests(Integer.parseInt(body.get("sleepRequests")));
            if (body.containsKey("retries"))
                appSettings.setRetries(Integer.parseInt(body.get("retries")));

            return ResponseEntity.ok(Map.of("message", "Settings updated successfully."));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid number format: " + e.getMessage()));
        }
    }

    // API: Lấy settings hiện tại
    @GetMapping("/api/settings")
    @ResponseBody
    public ResponseEntity<?> getSettings() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sleepInterval", appSettings.getSleepInterval());
        result.put("concurrentFragments", appSettings.getConcurrentFragments());
        result.put("sleepRequests", appSettings.getSleepRequests());
        result.put("retries", appSettings.getRetries());
        result.put("diskUsageMb", getDiskUsageMb());
        return ResponseEntity.ok(result);
    }

    // Tính dung lượng thư mục downloads (MB)
    private double getDiskUsageMb() {
        try {
            Path dir = Paths.get(downloadDir);
            if (!Files.exists(dir)) return 0;
            return Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> { try { return Files.size(p); } catch (Exception e) { return 0; } })
                    .sum() / (1024.0 * 1024.0);
        } catch (Exception e) {
            return 0;
        }
    }
}
