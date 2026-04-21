package com.example.platform.downloader.ui;

import com.example.platform.downloader.application.DownloaderDtoMapper;
import com.example.platform.downloader.application.SourceRequestService;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.JobState;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.ui.dto.JobStatusResponse;
import com.example.platform.kernel.exception.BusinessException;
import com.example.platform.kernel.exception.ResourceNotFoundException;
import com.example.platform.kernel.ui.RestResponse;
import com.example.platform.modules.user.domain.User;
import com.example.platform.modules.user.infrastructure.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiV1Controller {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AppSettings appSettings;
    private final SourceRequestService sourceRequestService;
    private final DownloaderDtoMapper dtoMapper;
    private final String downloadDir;
    private final String ytDlpPath;
    private final String ffmpegPath;

    public AdminApiV1Controller(JobRepository jobRepository,
                                UserRepository userRepository,
                                AppSettings appSettings,
                                SourceRequestService sourceRequestService,
                                DownloaderDtoMapper dtoMapper,
                                @Value("${app.downloader.output-dir:downloads}") String downloadDir,
                                @Value("${app.downloader.yt-dlp-path:yt-dlp}") String ytDlpPath,
                                @Value("${app.downloader.ffmpeg-path:ffmpeg}") String ffmpegPath) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.appSettings = appSettings;
        this.sourceRequestService = sourceRequestService;
        this.dtoMapper = dtoMapper;
        this.downloadDir = downloadDir;
        this.ytDlpPath = ytDlpPath;
        this.ffmpegPath = ffmpegPath;
    }

    @GetMapping("/dashboard")
    public RestResponse<Map<String, Object>> dashboard() {
        List<Job> jobs = jobRepository.findAllByOrderByCreatedAtDesc();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalJobs", jobs.size());
        result.put("completedJobs", jobs.stream().filter(job -> job.getStatus() == Job.JobStatus.COMPLETED).count());
        result.put("failedJobs", jobs.stream().filter(job -> job.getStatus() == Job.JobStatus.FAILED).count());
        result.put("userCount", userRepository.count());
        result.put("diskUsageMb", getDiskUsageMb());
        result.put("isYtDlpInstalled", checkBinary(ytDlpPath, "yt-dlp"));
        result.put("isFfmpegInstalled", checkBinary(ffmpegPath, "ffmpeg"));
        return RestResponse.ok(result);
    }

    @GetMapping("/jobs")
    public RestResponse<List<JobStatusResponse>> listJobs(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @RequestParam(required = false) JobState state,
                                                          @RequestParam(required = false) Job.JobStatus status,
                                                          @RequestParam(required = false) Platform platform) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Job> results = jobRepository.searchJobs(null, state, status, platform, pageable);
        RestResponse<List<JobStatusResponse>> response = RestResponse.ok(
                results.getContent().stream().map(this::toJobSummaryResponse).toList()
        );
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", results.getNumber());
        meta.put("size", results.getSize());
        meta.put("totalItems", results.getTotalElements());
        meta.put("totalPages", results.getTotalPages());
        meta.put("hasNext", results.hasNext());
        meta.put("hasPrevious", results.hasPrevious());
        response.setMeta(meta);
        return response;
    }

    @GetMapping("/users")
    public RestResponse<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(this::toUserSummary)
                .toList();
        return RestResponse.ok(users);
    }

    @GetMapping("/settings")
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

    @PutMapping("/settings")
    public RestResponse<Void> updateSettings(@RequestBody Map<String, String> body) {
        try {
            if (body.containsKey("sleepInterval")) {
                appSettings.setSleepInterval(Integer.parseInt(body.get("sleepInterval")));
            }
            if (body.containsKey("concurrentFragments")) {
                appSettings.setConcurrentFragments(Integer.parseInt(body.get("concurrentFragments")));
            }
            if (body.containsKey("sleepRequests")) {
                appSettings.setSleepRequests(Integer.parseInt(body.get("sleepRequests")));
            }
            if (body.containsKey("retries")) {
                appSettings.setRetries(Integer.parseInt(body.get("retries")));
            }
            if (body.containsKey("maxFileSizeMb")) {
                appSettings.setMaxFileSizeMb(Long.parseLong(body.get("maxFileSizeMb")));
            }
            return RestResponse.ok(null, "Settings updated");
        } catch (NumberFormatException e) {
            throw new BusinessException("Invalid number format: " + e.getMessage());
        }
    }

    @PostMapping("/jobs/{id}/resubmit")
    public RestResponse<Map<String, String>> resubmitJob(@PathVariable String id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (job.getUser() == null) {
            throw new BusinessException("Job does not have an owner.");
        }
        try {
            Job newJob = sourceRequestService.resubmit(job);
            return RestResponse.ok(Map.of("jobId", newJob.getId()), "Resubmitted");
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @PostMapping("/jobs/backfill-titles")
    public RestResponse<Map<String, Integer>> backfillTitles() {
        List<Job> jobs = jobRepository.findAll();
        int updated = 0;
        for (Job job : jobs) {
            if ((job.getVideoTitle() == null || job.getVideoTitle().isBlank()) && job.getExternalItemId() != null) {
                job.setVideoTitle(job.getExternalItemId());
                jobRepository.save(job);
                updated++;
            }
        }
        return RestResponse.ok(Map.of("updated", updated), "Backfill completed");
    }

    private boolean checkBinary(String path, String globalCmd) {
        if (new File(path).exists()) {
            return true;
        }
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
            if (!Files.exists(dir)) {
                return 0;
            }
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

    private Map<String, Object> toUserSummary(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("role", user.getRole().name());
        result.put("enabled", user.isEnabled());
        result.put("createdAt", user.getCreatedAt());
        return result;
    }

    private JobStatusResponse toJobSummaryResponse(Job job) {
        return dtoMapper.toJobStatus(job, List.of());
    }
}
