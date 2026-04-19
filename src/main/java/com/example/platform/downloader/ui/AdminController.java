package com.example.platform.downloader.ui;

import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.application.JobManager;
import com.example.platform.downloader.application.DownloaderService;
import com.example.platform.kernel.exception.BusinessException;
import com.example.platform.kernel.exception.ResourceNotFoundException;
import com.example.platform.kernel.ui.RestResponse;

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
    private final DownloaderService downloaderService;
    private final String downloadDir;

    public AdminController(JobManager jobManager,
                           JobRepository jobRepository,
                           UserRepository userRepository,
                           AppSettings appSettings,
                           DownloaderService downloaderService,
                           @Value("${app.downloader.output-dir:downloads}") String downloadDir) {
        this.jobManager = jobManager;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.appSettings = appSettings;
        this.downloaderService = downloaderService;
        this.downloadDir = downloadDir;
    }

    private boolean checkBinary(String path, String globalCmd) {
        if (new File(path).exists()) return true;
        try {
            Process p = new ProcessBuilder(globalCmd, "--version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
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
        
        // Dependency check
        model.addAttribute("isYtDlpInstalled", checkBinary("bin/yt-dlp.exe", "yt-dlp"));
        model.addAttribute("isFfmpegInstalled", checkBinary("bin/ffmpeg.exe", "ffmpeg"));
        
        return "admin";
    }

    // API: Tải lại (Re-submit) một Job đã hoàn thành hoặc thất bại
    @PostMapping("/api/job/{id}/resubmit")
    @ResponseBody
    public RestResponse<Map<String, String>> resubmitJob(@PathVariable String id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (job.getUser() == null) throw new BusinessException("Job không có người dùng.");

        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("url", job.getUrl());
            payload.put("type", job.getDownloadType() != null ? job.getDownloadType() : "VIDEO");
            payload.put("quality", job.getQuality() != null ? job.getQuality() : "best");
            payload.put("format", job.getFormat() != null ? job.getFormat() : "mp4");
            if (job.getProxy() != null) payload.put("proxy", job.getProxy());
            if (job.getStartTime() != null) payload.put("startTime", job.getStartTime());
            if (job.getEndTime() != null) payload.put("endTime", job.getEndTime());
            payload.put("cleanMetadata", job.isCleanMetadata() ? "true" : "false");
            payload.put("writeThumbnail", job.isWriteThumbnail() ? "true" : "false");
            if (job.getWatermarkText() != null) payload.put("watermarkText", job.getWatermarkText());
            if (job.getTitleTemplate() != null) payload.put("titleTemplate", job.getTitleTemplate());

            Job newJob = downloaderService.submitDownload(payload, job.getUser());
            return RestResponse.ok(null, "✅ Đã tải lại thành công! Job mới: " + newJob.getId().substring(0, 8) + "...");
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }


    // API: Tự động lấy tiêu đề cho tất cả job chưa có title (gọi 1 lần sau deploy)
    @PostMapping("/api/jobs/backfill-titles")
    @ResponseBody
    public RestResponse<Void> backfillTitles() {
        List<Job> jobs = jobRepository.findAll();
        int updated = 0;
        for (Job job : jobs) {
            if (job.getVideoTitle() == null || job.getVideoTitle().isBlank()) {
                String title = downloaderService.fetchVideoTitle(job.getUrl());
                if (title != null && !title.isBlank()) {
                    job.setVideoTitle(title);
                    jobRepository.save(job);
                    updated++;
                }
            }
        }
        return RestResponse.ok(null, "✅ Đã cập nhật tiêu đề cho " + updated + " job.");
    }

    // API: Cập nhật cấu hình hệ thống (nóng - không cần restart)
    @PostMapping("/api/settings")
    @ResponseBody
    public RestResponse<Void> updateSettings(@RequestBody Map<String, String> body) {
        try {
            if (body.containsKey("sleepInterval"))
                appSettings.setSleepInterval(Integer.parseInt(body.get("sleepInterval")));
            if (body.containsKey("concurrentFragments"))
                appSettings.setConcurrentFragments(Integer.parseInt(body.get("concurrentFragments")));
            if (body.containsKey("sleepRequests"))
                appSettings.setSleepRequests(Integer.parseInt(body.get("sleepRequests")));
            if (body.containsKey("retries"))
                appSettings.setRetries(Integer.parseInt(body.get("retries")));

            return RestResponse.ok(null, "Cấu hình đã được cập nhật thành công.");
        } catch (NumberFormatException e) {
            throw new BusinessException("Định dạng số không hợp lệ: " + e.getMessage());
        }
    }

    // API: Lấy settings hiện tại
    @GetMapping("/api/settings")
    @ResponseBody
    public RestResponse<Map<String, Object>> getSettings() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sleepInterval", appSettings.getSleepInterval());
        result.put("concurrentFragments", appSettings.getConcurrentFragments());
        result.put("sleepRequests", appSettings.getSleepRequests());
        result.put("retries", appSettings.getRetries());
        result.put("diskUsageMb", getDiskUsageMb());
        return RestResponse.ok(result);
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
