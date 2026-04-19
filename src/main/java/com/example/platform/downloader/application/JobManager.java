package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.domain.RuntimeSettings;
import com.example.platform.downloader.infrastructure.JobRepository;

import org.springframework.stereotype.Service;
import java.util.concurrent.*;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;

import jakarta.servlet.http.HttpSession;

@Service
public class JobManager {

    // Concurrency limit: 2 downloads at a time
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    // Scheduler để dọn activeJobs sau 30 phút (fix memory leak)
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Map<String, Job> activeJobs = new ConcurrentHashMap<>();

    private final JobRepository jobRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final DownloaderService downloaderService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public JobManager(JobRepository jobRepository,
                      TelegramNotificationService telegramNotificationService,
                      @Lazy DownloaderService downloaderService) {
        this.jobRepository = jobRepository;
        this.telegramNotificationService = telegramNotificationService;
        this.downloaderService = downloaderService;
    }

    /**
     * Submit một download job mới.
     *
     * @param url         URL cần tải
     * @param currentUser User thực hiện yêu cầu
     * @param setupJob    Lambda SET field (chạy SYNC trên HTTP thread TRƯỚC khi save DB) — fix Bug 2
     * @param executeJob  Lambda CHẠY download (chạy ASYNC trong executor thread)
     * @param session     HttpSession của request — dùng để lấy RuntimeSettings cho Telegram
     * @return Job đã được tạo và persist với đầy đủ thông tin
     */
    public Job submitJob(String url,
                         com.example.platform.modules.user.domain.User currentUser,
                         Consumer<Job> setupJob,
                         Runnable executeJob,
                         HttpSession session) {
        Job job = new Job(url);
        job.setUser(currentUser);

        // Bug 2 Fix: set tất cả field TRƯỚC khi save lần đầu
        setupJob.accept(job);
        jobRepository.save(job); // record đầy đủ type/quality/format ngay từ đầu

        activeJobs.put(job.getId(), job);

        executor.submit(() -> {
            job.setStatus(Job.JobStatus.RUNNING);
            jobRepository.save(job);
            try {
                executeJob.run();
                job.setStatus(Job.JobStatus.COMPLETED);
            } catch (Exception e) {
                job.setStatus(Job.JobStatus.FAILED);
                job.addLog("Error: " + e.getMessage());
            } finally {
                jobRepository.save(job);
                // Telegram notification — token lấy từ session (không hardcode yml)
                try {
                    if (job.getUser() != null) {
                        // Resolve baseUrl: ưu tiên runtime setting, fallback về @Value
                        String resolvedBaseUrl = resolveBaseUrl(session);
                        if (job.getStatus() == Job.JobStatus.COMPLETED) {
                            var files = downloaderService.listJobFiles(job.getId());
                            telegramNotificationService.notifyJobCompleted(
                                    job, job.getUser(), files, resolvedBaseUrl, session);
                        } else if (job.getStatus() == Job.JobStatus.FAILED) {
                            telegramNotificationService.notifyJobFailed(job, job.getUser(), session);
                        }
                    }
                } catch (Exception ignored) {
                    // Notification failure không được làm fail job
                }
                // Bug 1 Fix: dọn khỏi activeJobs sau 30 phút để tránh memory leak
                scheduler.schedule(
                        () -> activeJobs.remove(job.getId()),
                        30, TimeUnit.MINUTES
                );
            }
        });

        return job;
    }

    /**
     * Lấy job theo ID. Active jobs (đang chạy) được ưu tiên vì có logs in-memory.
     * Bug 3 Fix: khi load từ DB, khởi tạo logs = [] thay vì trả null về FE.
     */
    public Job getJob(String id) {
        // First try active memory (has live logs)
        if (activeJobs.containsKey(id)) {
            return activeJobs.get(id);
        }
        // Fallback to database — initialize transient fields to safe defaults
        Job job = jobRepository.findById(id).orElse(null);
        if (job != null && job.getLogs() == null) {
            job.setLogs(new ArrayList<>());
        }
        return job;
    }

    /**
     * Per-user lock để serialize quota check + submit, ngăn concurrent bypass (Bug 4).
     *
     * Dùng String.intern() thay Map<userId, Object> để JVM tự quản lý string pool,
     * tránh Map grow vô hạn với mỗi userId mới.
     */
    public Object getUserLock(String userId) {
        return userId.intern();
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll().stream()
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ưu tiên baseUrl từ RuntimeSettings trong session.
     * Nếu không có thì dùng giá trị từ application.yml (@Value).
     */
    private String resolveBaseUrl(HttpSession session) {
        if (session != null) {
            try {
                Object attr = session.getAttribute(SessionSettingsService.SESSION_KEY);
                if (attr instanceof RuntimeSettings rs
                        && rs.getBaseUrl() != null && !rs.getBaseUrl().isBlank()) {
                    return rs.getBaseUrl();
                }
            } catch (IllegalStateException ignored) {
                // Session invalidated
            }
        }
        return baseUrl; // fallback về @Value từ yml
    }
}
