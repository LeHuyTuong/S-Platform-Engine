package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.FailureCategory;
import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.domain.JobState;
import com.example.platform.downloader.domain.Platform;
import com.example.platform.downloader.infrastructure.DownloadAttemptRepository;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.WorkerProperties;
import com.example.platform.modules.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadWorkerServiceTest {

    @Mock
    private SourceRequestService sourceRequestService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private DownloadAttemptRepository downloadAttemptRepository;

    @Mock
    private DownloaderService downloaderService;

    @Mock
    private DownloadArtifactService downloadArtifactService;

    @Mock
    private JobEventService jobEventService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    @Mock
    private DownloaderMetricsService downloaderMetricsService;

    @Mock
    private UserRepository userRepository;

    private DownloadWorkerService downloadWorkerService;

    @BeforeEach
    void setUp() {
        WorkerProperties workerProperties = new WorkerProperties();
        workerProperties.setWorkerId("test-worker");
        workerProperties.setConcurrency(4);
        workerProperties.setRetryBackoffSeconds(List.of(60, 300, 900));
        workerProperties.setProviderLimits(new HashMap<>(java.util.Map.of("youtube", 2)));

        downloadWorkerService = new DownloadWorkerService(
                sourceRequestService,
                jobRepository,
                downloadAttemptRepository,
                downloaderService,
                downloadArtifactService,
                jobEventService,
                outboxService,
                telegramNotificationService,
                downloaderMetricsService,
                workerProperties,
                userRepository
        );
    }

    @Test
    void retryableFailureMovesJobToRetryWaitAndEnqueuesDelayedRetry() {
        Job job = new Job("https://www.youtube.com/watch?v=abc123");
        job.setPlatform(Platform.YOUTUBE);
        job.setState(JobState.QUEUED);
        job.setStatus(Job.JobStatus.PENDING);
        job.setMaxAttempts(4);

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.acquireLease(eq(job.getId()), eq("test-worker"), any(), eq(JobState.RUNNING),
                eq(Job.JobStatus.RUNNING), anyList(), any())).thenReturn(1);
        when(downloadAttemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ClassifiedDownloadException(FailureCategory.TIMEOUT, "timeout"))
                .when(downloaderService).executeDownload(eq(job), any(Duration.class));

        downloadWorkerService.handleDownloadJob(job.getId());

        assertThat(job.getState()).isEqualTo(JobState.RETRY_WAIT);
        assertThat(job.getStatus()).isEqualTo(Job.JobStatus.PENDING);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getFailureCategory()).isEqualTo(FailureCategory.TIMEOUT);
        assertThat(job.getNextAttemptAt()).isNotNull();
        verify(downloadArtifactService).cleanupFailedArtifacts(job);
        verify(outboxService).create(eq("job"), eq(job.getId()), eq("DOWNLOAD_JOB_QUEUED"), anyMap(), eq(60));
        verify(downloaderMetricsService).recordRetry(job, FailureCategory.TIMEOUT);
        verify(telegramNotificationService, never()).notifyJobFailed(any(), any());
    }
}
