package com.example.platform.downloader.ui;

import com.example.platform.downloader.application.SourceRequestService;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.kernel.exception.BusinessException;
import com.example.platform.kernel.exception.ResourceNotFoundException;
import com.example.platform.kernel.ui.RestResponse;
import com.example.platform.modules.user.infrastructure.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AppSettings appSettings;
    private final SourceRequestService sourceRequestService;
    private final String downloadDir;
    private final String ytDlpPath;
    private final String ffmpegPath;

    public AdminController(JobRepository jobRepository,
                           UserRepository userRepository,
                           AppSettings appSettings,
                           SourceRequestService sourceRequestService,
                           @Value("${app.downloader.output-dir:downloads}") String downloadDir,
                           @Value("${app.downloader.yt-dlp-path:yt-dlp}") String ytDlpPath,
                           @Value("${app.downloader.ffmpeg-path:ffmpeg}") String ffmpegPath) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.appSettings = appSettings;
        this.sourceRequestService = sourceRequestService;
        this.downloadDir = downloadDir;
        this.ytDlpPath = ytDlpPath;
        this.ffmpegPath = ffmpegPath;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("jobs", jobRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("settings", appSettings);
        model.addAttribute("diskUsage", getDiskUsageMb());
        model.addAttribute("isYtDlpInstalled", checkBinary(ytDlpPath, "yt-dlp"));
        model.addAttribute("isFfmpegInstalled", checkBinary(ffmpegPath, "ffmpeg"));
        return "admin";
    }

    @PostMapping("/api/job/{id}/resubmit")
    @ResponseBody
    public RestResponse<Map<String, String>> resubmitJob(@PathVariable String id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (job.getUser() == null) {
            throw new BusinessException("Job không có người dùng.");
        }
        try {
            Job newJob = sourceRequestService.resubmit(job);
            return RestResponse.ok(Map.of("jobId", newJob.getId()), "Đã tạo job mới: " + newJob.getId());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @PostMapping("/api/jobs/backfill-titles")
    @ResponseBody
    public RestResponse<Void> backfillTitles() {
        List<Job> jobs = jobRepository.findAll();
        int updated = 0;
        for (Job job : jobs) {
            if ((job.getVideoTitle() == null || job.getVideoTitle().isBlank()) && job.getExternalItemId() != null) {
                job.setVideoTitle(job.getExternalItemId());
                jobRepository.save(job);
                updated++;
            }
        }
        return RestResponse.ok(null, "Đã cập nhật tiêu đề cho " + updated + " job.");
    }

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
            if (body.containsKey("maxFileSizeMb"))
                appSettings.setMaxFileSizeMb(Long.parseLong(body.get("maxFileSizeMb")));
            return RestResponse.ok(null, "Cấu hình đã được cập nhật thành công.");
        } catch (NumberFormatException e) {
            throw new BusinessException("Định dạng số không hợp lệ: " + e.getMessage());
        }
    }

    @GetMapping("/api/settings")
    @ResponseBody
    public RestResponse<Map<String, Object>> getSettings() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sleepInterval", appSettings.getSleepInterval());
        result.put("concurrentFragments", appSettings.getConcurrentFragments());
        result.put("sleepRequests", appSettings.getSleepRequests());
        result.put("retries", appSettings.getRetries());
        result.put("maxFileSizeMb", appSettings.getMaxFileSizeMb());
        result.put("diskUsageMb", getDiskUsageMb());
        return RestResponse.ok(result);
    }

    private boolean checkBinary(String path, String globalCmd) {
        if (new File(path).exists()) return true;
        try {
            Process process = new ProcessBuilder(globalCmd, "--version").start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private double getDiskUsageMb() {
        try {
            Path dir = Paths.get(downloadDir);
            if (!Files.exists(dir)) return 0;
            return Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum() / (1024.0 * 1024.0);
        } catch (Exception e) {
            return 0;
        }
    }
}
