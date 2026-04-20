package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.FailureCategory;
import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.domain.JobState;
import com.example.platform.downloader.domain.OutboxStatus;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class DownloaderMetricsService {

    private final MeterRegistry meterRegistry;

    public DownloaderMetricsService(MeterRegistry meterRegistry,
                                    JobRepository jobRepository,
                                    OutboxEventRepository outboxEventRepository) {
        this.meterRegistry = meterRegistry;

        Gauge.builder("downloader.jobs.queue.depth", jobRepository,
                        repo -> repo.countByStateIn(List.of(JobState.QUEUED, JobState.RETRY_WAIT)))
                .description("Jobs waiting to be processed or retried")
                .register(meterRegistry);

        Gauge.builder("downloader.outbox.pending.depth", outboxEventRepository,
                        repo -> repo.countByStatus(OutboxStatus.PENDING))
                .description("Pending outbox events awaiting dispatch")
                .register(meterRegistry);
    }

    public void recordJobSuccess(Job job, Duration duration) {
        Timer.builder("downloader.job.duration")
                .description("End-to-end duration for completed download jobs")
                .tag("platform", platform(job))
                .tag("result", "success")
                .register(meterRegistry)
                .record(duration);

        Counter.builder("downloader.job.completed.total")
                .description("Completed download jobs")
                .tag("platform", platform(job))
                .register(meterRegistry)
                .increment();
    }

    public void recordJobFailure(Job job, FailureCategory category, Duration duration) {
        Timer.builder("downloader.job.duration")
                .description("End-to-end duration for failed download jobs")
                .tag("platform", platform(job))
                .tag("result", "failure")
                .register(meterRegistry)
                .record(duration);

        Counter.builder("downloader.job.failed.total")
                .description("Failed download jobs grouped by provider error class")
                .tag("platform", platform(job))
                .tag("failure_category", category == null ? "UNKNOWN" : category.name())
                .register(meterRegistry)
                .increment();
    }

    public void recordRetry(Job job, FailureCategory category) {
        Counter.builder("downloader.job.retry.total")
                .description("Retried download jobs")
                .tag("platform", platform(job))
                .tag("failure_category", category == null ? "UNKNOWN" : category.name())
                .register(meterRegistry)
                .increment();
    }

    public void recordLeaseRecovery() {
        Counter.builder("downloader.job.lease.recovered.total")
                .description("Recovered orphaned or expired job leases")
                .register(meterRegistry)
                .increment();
    }

    private String platform(Job job) {
        return job.getPlatform() == null ? "UNKNOWN" : job.getPlatform().name();
    }
}
