package com.example.platform.downloader.application.job;

import com.example.platform.downloader.application.DownloadArtifactService;
import com.example.platform.downloader.application.DownloaderMetricsService;
import com.example.platform.downloader.application.DownloaderService;
import com.example.platform.downloader.application.SourceRequestService;
import com.example.platform.downloader.application.TelegramNotificationService;
import com.example.platform.downloader.application.outbox.OutboxService;
import com.example.platform.downloader.domain.entity.DownloadAttempt;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.FailureCategory;
import com.example.platform.downloader.domain.enums.JobState;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.exception.ClassifiedDownloadException;
import com.example.platform.downloader.infrastructure.DownloadAttemptRepository;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.WorkerProperties;
import com.example.platform.modules.user.domain.User;
import com.example.platform.modules.user.infrastructure.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
/**
 * Thành phần thực thi nền cho bước resolve và download.
 *
 * Nó dựa vào:
 * - outbox event để nhận việc
 * - DB lease trên job để tránh chạy trùng
 * - metadata retry trên `Job` và `DownloadAttempt` để tự phục hồi sau lỗi tạm thời
 */
public class DownloadWorkerService {

    private final SourceRequestService sourceRequestService;
    private final JobRepository jobRepository;
    private final DownloadAttemptRepository downloadAttemptRepository;
    private final DownloaderService downloaderService;
    private final DownloadArtifactService downloadArtifactService;
    private final JobEventService jobEventService;
    private final OutboxService outboxService;
    private final TelegramNotificationService telegramNotificationService;
    private final DownloaderMetricsService downloaderMetricsService;
    private final WorkerProperties workerProperties;
    private final UserRepository userRepository;
    private final Semaphore globalSemaphore;
    private final Map<Platform, Semaphore> semaphores = new EnumMap<>(Platform.class);

    public DownloadWorkerService(SourceRequestService sourceRequestService,
                                 JobRepository jobRepository,
                                 DownloadAttemptRepository downloadAttemptRepository,
                                 DownloaderService downloaderService,
                                 DownloadArtifactService downloadArtifactService,
                                 JobEventService jobEventService,
                                 OutboxService outboxService,
                                 TelegramNotificationService telegramNotificationService,
                                 DownloaderMetricsService downloaderMetricsService,
                                 WorkerProperties workerProperties,
                                 UserRepository userRepository) {
        this.sourceRequestService = sourceRequestService;
        this.jobRepository = jobRepository;
        this.downloadAttemptRepository = downloadAttemptRepository;
        this.downloaderService = downloaderService;
        this.downloadArtifactService = downloadArtifactService;
        this.jobEventService = jobEventService;
        this.outboxService = outboxService;
        this.telegramNotificationService = telegramNotificationService;
        this.downloaderMetricsService = downloaderMetricsService;
        this.workerProperties = workerProperties;
        this.userRepository = userRepository;
        this.globalSemaphore = new Semaphore(Math.max(1, workerProperties.getConcurrency()));

        for (Platform platform : Platform.values()) {
            int permits = workerProperties.getProviderLimits()
                    .getOrDefault(platform.name().toLowerCase(), 1);
            semaphores.put(platform, new Semaphore(Math.max(1, permits)));
        }
    }

    public void handleSourceRequest(String sourceRequestId) {
        sourceRequestService.resolveSourceRequest(sourceRequestId);
    }

    public void handleDownloadJob(String jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getPlatform() == null) {
            return;
        }

        // Global semaphore thực thi giới hạn tổng số job worker chạy cùng lúc.
        if (!globalSemaphore.tryAcquire()) {
            outboxService.create("job", jobId, "DOWNLOAD_JOB_QUEUED", Map.of("jobId", jobId), 15);
            return;
        }

        // Semaphore theo provider giúp chặn bớt concurrency trước cả khi claim DB lease.
        Semaphore semaphore = semaphores.getOrDefault(job.getPlatform(), new Semaphore(1));
        if (!semaphore.tryAcquire()) {
            globalSemaphore.release();
            outboxService.create("job", jobId, "DOWNLOAD_JOB_QUEUED", Map.of("jobId", jobId), 15);
            return;
        }

        try {
            executeClaimedJob(jobId);
        } finally {
            semaphore.release();
            globalSemaphore.release();
        }
    }

    protected void executeClaimedJob(String jobId) {
        // Bước update lease là lớp bảo vệ cuối cùng để nhiều worker không chạy cùng một job.
        LocalDateTime now = LocalDateTime.now();
        int acquired = jobRepository.acquireLease(
                jobId,
                workerProperties.getWorkerId(),
                now.plusMinutes(workerProperties.getDownloadTimeoutMinutes()),
                JobState.RUNNING,
                Job.JobStatus.RUNNING,
                List.of(JobState.QUEUED, JobState.RETRY_WAIT),
                now
        );
        if (acquired == 0) {
            return;
        }

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        job.setState(JobState.RUNNING);
        job.setStatus(Job.JobStatus.RUNNING);
        job.setStartedAt(now);
        jobRepository.save(job);
        jobEventService.appendInfo(job, "[WORKER] Claimed by " + workerProperties.getWorkerId());

        DownloadAttempt attempt = new DownloadAttempt();
        attempt.setJob(job);
        attempt.setAttemptNumber(job.getAttemptCount() + 1);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setSuccess(false);
        downloadAttemptRepository.save(attempt);

        try {
            downloaderService.executeDownload(job, Duration.ofMinutes(workerProperties.getDownloadTimeoutMinutes()));
            attempt.setSuccess(true);
            attempt.setFinishedAt(LocalDateTime.now());
            downloadAttemptRepository.save(attempt);

            job.setAttemptCount(attempt.getAttemptNumber());
            job.setState(JobState.COMPLETED);
            job.setStatus(Job.JobStatus.COMPLETED);
            job.setFinishedAt(LocalDateTime.now());
            job.setLeaseOwner(null);
            job.setLeaseExpiresAt(null);
            jobRepository.save(job);
            downloaderMetricsService.recordJobSuccess(job, jobDuration(job));
            telegramNotificationService.notifyJobCompleted(
                    job,
                    resolveNotificationUser(job),
                    downloadArtifactService.listJobFiles(job.getId())
            );
        } catch (ClassifiedDownloadException e) {
            attempt.setFailureCategory(e.getFailureCategory());
            attempt.setErrorMessage(e.getMessage());
            attempt.setFinishedAt(LocalDateTime.now());
            downloadAttemptRepository.save(attempt);

            job.setAttemptCount(attempt.getAttemptNumber());
            job.setFailureCategory(e.getFailureCategory());
            job.setErrorMessage(e.getMessage());
            job.setLeaseOwner(null);
            job.setLeaseExpiresAt(null);

            if (isRetryable(e.getFailureCategory()) && attempt.getAttemptNumber() < job.getMaxAttempts()) {
                // Lỗi có thể retry sẽ giữ nguyên job hiện tại và lên lịch requeue có delay.
                downloadArtifactService.cleanupFailedArtifacts(job);
                int delaySeconds = retryDelay(attempt.getAttemptNumber());
                job.setState(JobState.RETRY_WAIT);
                job.setStatus(Job.JobStatus.PENDING);
                job.setNextAttemptAt(LocalDateTime.now().plusSeconds(delaySeconds));
                jobEventService.appendWarn(
                        job,
                        "[WORKER] Retry scheduled in " + delaySeconds + "s due to " + e.getFailureCategory()
                );
                jobRepository.save(job);
                downloaderMetricsService.recordRetry(job, e.getFailureCategory());
                outboxService.create("job", job.getId(), "DOWNLOAD_JOB_QUEUED", Map.of("jobId", job.getId()), delaySeconds);
            } else {
                downloadArtifactService.cleanupFailedArtifacts(job);
                job.setState(isBlockedFailure(e.getFailureCategory()) ? JobState.BLOCKED : JobState.FAILED);
                job.setStatus(Job.JobStatus.FAILED);
                job.setFinishedAt(LocalDateTime.now());
                jobRepository.save(job);
                downloaderMetricsService.recordJobFailure(job, e.getFailureCategory(), jobDuration(job));
                telegramNotificationService.notifyJobFailed(job, resolveNotificationUser(job));
            }
        } catch (Exception e) {
            attempt.setFailureCategory(FailureCategory.UNKNOWN);
            attempt.setErrorMessage(e.getMessage());
            attempt.setFinishedAt(LocalDateTime.now());
            downloadAttemptRepository.save(attempt);

            job.setAttemptCount(attempt.getAttemptNumber());
            job.setFailureCategory(FailureCategory.UNKNOWN);
            job.setErrorMessage(e.getMessage());
            job.setState(JobState.FAILED);
            job.setStatus(Job.JobStatus.FAILED);
            job.setFinishedAt(LocalDateTime.now());
            job.setLeaseOwner(null);
            job.setLeaseExpiresAt(null);
            downloadArtifactService.cleanupFailedArtifacts(job);
            jobRepository.save(job);
            downloaderMetricsService.recordJobFailure(job, FailureCategory.UNKNOWN, jobDuration(job));
            telegramNotificationService.notifyJobFailed(job, resolveNotificationUser(job));
        }
    }

    public void recoverExpiredRunningJobs() {
        // Safety net khi worker chết giữa chừng: job `RUNNING` hết lease sẽ quay lại luồng retry.
        for (Job job : jobRepository.findExpiredRunningJobs(JobState.RUNNING, LocalDateTime.now())) {
            job.setLeaseOwner(null);
            job.setLeaseExpiresAt(null);
            job.setState(JobState.RETRY_WAIT);
            job.setStatus(Job.JobStatus.PENDING);
            job.setNextAttemptAt(LocalDateTime.now().plusSeconds(retryDelay(Math.max(1, job.getAttemptCount()))));
            jobRepository.save(job);
            downloaderMetricsService.recordLeaseRecovery();
            outboxService.create("job", job.getId(), "DOWNLOAD_JOB_QUEUED", Map.of("jobId", job.getId()), 30);
        }
    }

    private boolean isRetryable(FailureCategory category) {
        return category == FailureCategory.RATE_LIMIT
                || category == FailureCategory.TEMPORARY
                || category == FailureCategory.TIMEOUT
                || category == FailureCategory.PROCESS_ERROR
                || category == FailureCategory.UNKNOWN;
    }

    private boolean isBlockedFailure(FailureCategory category) {
        return category == FailureCategory.INVALID_URL
                || category == FailureCategory.PRIVATE_CONTENT
                || category == FailureCategory.REMOVED
                || category == FailureCategory.PERMISSION_DENIED
                || category == FailureCategory.UNSUPPORTED;
    }

    private int retryDelay(int attemptNumber) {
        List<Integer> backoffs = workerProperties.getRetryBackoffSeconds();
        if (backoffs == null || backoffs.isEmpty()) {
            return 60;
        }
        int index = Math.min(Math.max(attemptNumber - 1, 0), backoffs.size() - 1);
        return backoffs.get(index);
    }

    private Duration jobDuration(Job job) {
        if (job.getStartedAt() == null || job.getFinishedAt() == null) {
            return Duration.ZERO;
        }
        return Duration.between(job.getStartedAt(), job.getFinishedAt());
    }

    private User resolveNotificationUser(Job job) {
        if (job.getUser() == null || job.getUser().getId() == null) {
            return null;
        }
        return userRepository.findById(job.getUser().getId()).orElse(null);
    }
}
