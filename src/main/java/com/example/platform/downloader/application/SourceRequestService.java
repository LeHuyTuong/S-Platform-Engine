package com.example.platform.downloader.application;

import com.example.platform.downloader.application.outbox.OutboxService;
import com.example.platform.downloader.application.provider.ContentProvider;
import com.example.platform.downloader.application.provider.ProviderRegistry;
import com.example.platform.downloader.application.provider.ResolvedItem;
import com.example.platform.downloader.application.provider.SourceResolution;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.JobState;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.domain.entity.SourceRequest;
import com.example.platform.downloader.domain.enums.SourceRequestState;
import com.example.platform.downloader.domain.enums.SourceType;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.SourceRequestRepository;
import com.example.platform.downloader.ui.dto.SubmitSourceRequest;
import com.example.platform.modules.user.domain.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
/**
 * Chuyển input từ API thành dữ liệu bền vững để pipeline downloader xử lý.
 *
 * Flow chính:
 * 1. submit() lưu SourceRequest ngay
 * 2. DIRECT_URL tạo Job luôn
 * 3. PLAYLIST/PROFILE chỉ phát outbox event, worker sẽ resolve và fan-out Job sau
 * 4. resubmit() dựng lại request/job mới từ snapshot của job cũ
 */
public class SourceRequestService {

    private final SourceRequestRepository sourceRequestRepository;
    private final JobRepository jobRepository;
    private final ProviderRegistry providerRegistry;
    private final OutboxService outboxService;

    public SourceRequestService(SourceRequestRepository sourceRequestRepository,
                                JobRepository jobRepository,
                                ProviderRegistry providerRegistry,
                                OutboxService outboxService) {
        this.sourceRequestRepository = sourceRequestRepository;
        this.jobRepository = jobRepository;
        this.providerRegistry = providerRegistry;
        this.outboxService = outboxService;
    }

    @Transactional
    public SubmissionResult submit(SubmitSourceRequest request, User user) {
        // Mọi lựa chọn từ HTTP request phải được copy sang DB ngay tại đây để worker nền không cần đọc session/web state.
        String sourceUrl = request.effectiveSourceUrl();
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập URL");
        }

        ContentProvider provider = resolveProvider(request.getPlatform(), sourceUrl);
        SourceType sourceType = resolveSourceType(request.getSourceType(), sourceUrl);

        SourceRequest sourceRequest = new SourceRequest();
        sourceRequest.setUser(user);
        sourceRequest.setPlatform(provider.platform());
        sourceRequest.setSourceType(sourceType);
        sourceRequest.setSourceUrl(sourceUrl);
        sourceRequest.setNormalizedUrl(sourceUrl.strip());
        sourceRequest.setRequestedDownloadType(defaultText(request.getDownloadType(), "VIDEO"));
        sourceRequest.setRequestedQuality(defaultText(request.getQuality(), "best"));
        sourceRequest.setRequestedFormat(defaultText(request.getFormat(), "mp4"));
        sourceRequest.setProxyRef(blankToNull(request.getProxyRef()));
        sourceRequest.setProxy(blankToNull(request.getProxy()));
        sourceRequest.setStartTime(blankToNull(request.getStartTime()));
        sourceRequest.setEndTime(blankToNull(request.getEndTime()));
        sourceRequest.setCleanMetadata(request.isCleanMetadata());
        sourceRequest.setWriteThumbnail(request.isWriteThumbnail());
        sourceRequest.setWatermarkText(blankToNull(request.getWatermarkText()));
        sourceRequest.setTitleTemplate(blankToNull(request.getTitleTemplate()));

        Job primaryJob = null;
        if (sourceType == SourceType.DIRECT_URL) {
            sourceRequest.setState(SourceRequestState.RESOLVED);
            sourceRequest.setResolvedCount(1);
        }

        sourceRequestRepository.save(sourceRequest);

        if (sourceType == SourceType.DIRECT_URL) {
            primaryJob = createJob(sourceRequest, provider.platform(), sourceUrl, null, request, null);
            jobRepository.save(primaryJob);
            outboxService.create("job", primaryJob.getId(), "DOWNLOAD_JOB_QUEUED", Map.of("jobId", primaryJob.getId()));
        } else {
            outboxService.create("source_request", sourceRequest.getId(), "SOURCE_REQUEST_ACCEPTED",
                    Map.of("sourceRequestId", sourceRequest.getId()));
        }

        return new SubmissionResult(sourceRequest, primaryJob);
    }

    @Transactional
    public List<Job> resolveSourceRequest(String sourceRequestId) {
        // Bước resolve ở phía worker: provider bung playlist/profile thành các item media cụ thể.
        SourceRequest sourceRequest = sourceRequestRepository.findById(sourceRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Source request not found: " + sourceRequestId));
        ContentProvider provider = providerRegistry.byPlatform(sourceRequest.getPlatform());

        sourceRequest.setState(SourceRequestState.RESOLVING);
        sourceRequestRepository.save(sourceRequest);

        SourceResolution resolution = provider.resolveSource(sourceRequest);
        if (resolution.isBlocked()) {
            sourceRequest.setState(SourceRequestState.BLOCKED);
            sourceRequest.setBlockedReason(resolution.getBlockedReason());
            sourceRequestRepository.save(sourceRequest);
            return List.of();
        }

        List<Job> jobs = new ArrayList<>();
        int created = 0;
        for (ResolvedItem item : resolution.getItems()) {
            if (item.getExternalItemId() != null && !item.getExternalItemId().isBlank()) {
                String requestedVariant = requestedVariant(sourceRequest);
                boolean duplicate = jobRepository.existsByUserIdAndPlatformAndExternalItemIdAndRequestedVariant(
                        sourceRequest.getUser().getId(),
                        sourceRequest.getPlatform(),
                        item.getExternalItemId(),
                        requestedVariant
                );
                if (duplicate) {
                    continue;
                }
            }
            Job job = createJob(sourceRequest, sourceRequest.getPlatform(), item.getSourceUrl(), item, null, resolution);
            jobs.add(jobRepository.save(job));
            outboxService.create("job", job.getId(), "DOWNLOAD_JOB_QUEUED", Map.of("jobId", job.getId()));
            created++;
        }

        sourceRequest.setResolvedCount(created);
        sourceRequest.setState(SourceRequestState.RESOLVED);
        sourceRequestRepository.save(sourceRequest);
        return jobs;
    }

    @Transactional
    public Job resubmit(Job previousJob) {
        SubmitSourceRequest request = new SubmitSourceRequest();
        request.setSourceUrl(previousJob.getUrl());
        request.setPlatform(previousJob.getPlatform() != null ? previousJob.getPlatform().name() : null);
        request.setSourceType(previousJob.getSourceType() != null ? previousJob.getSourceType().name() : null);
        request.setDownloadType(previousJob.getDownloadType());
        request.setQuality(previousJob.getQuality());
        request.setFormat(previousJob.getFormat());
        request.setWriteThumbnail(previousJob.isWriteThumbnail());
        request.setCleanMetadata(previousJob.isCleanMetadata());
        request.setStartTime(previousJob.getStartTime());
        request.setEndTime(previousJob.getEndTime());
        request.setProxy(previousJob.getProxy());
        request.setProxyRef(previousJob.getProxyRef());
        request.setTitleTemplate(previousJob.getTitleTemplate());
        request.setWatermarkText(previousJob.getWatermarkText());

        return submit(request, previousJob.getUser()).primaryJob();
    }

    private Job createJob(SourceRequest sourceRequest, Platform platform, String url, ResolvedItem resolvedItem,
                          SubmitSourceRequest request, SourceResolution resolution) {
        // Job là payload đầy đủ cho worker, được materialize từ SourceRequest + metadata resolve được.
        Job job = new Job(url);
        job.setUser(sourceRequest.getUser());
        job.setSourceRequest(sourceRequest);
        job.setPlatform(platform);
        job.setSourceType(sourceRequest.getSourceType());
        job.setState(JobState.QUEUED);
        job.setQueuedAt(LocalDateTime.now());
        job.setStatus(Job.JobStatus.PENDING);
        job.setDownloadType(sourceRequest.getRequestedDownloadType());
        job.setQuality(sourceRequest.getRequestedQuality());
        job.setFormat(sourceRequest.getRequestedFormat());
        job.setRequestedVariant(requestedVariant(sourceRequest));
        job.setProxy(sourceRequest.getProxy());
        job.setProxyRef(sourceRequest.getProxyRef());
        job.setStartTime(sourceRequest.getStartTime());
        job.setEndTime(sourceRequest.getEndTime());
        job.setCleanMetadata(sourceRequest.isCleanMetadata());
        job.setWriteThumbnail(sourceRequest.isWriteThumbnail());
        job.setTitleTemplate(sourceRequest.getTitleTemplate());
        job.setWatermarkText(sourceRequest.getWatermarkText());
        job.setAttemptCount(0);
        job.setMaxAttempts(4);

        if (resolvedItem != null) {
            job.setExternalItemId(resolvedItem.getExternalItemId());
            job.setVideoTitle(resolvedItem.getTitle());
            job.setAuthorName(resolvedItem.getAuthor());
            job.setCaptionText(resolvedItem.getCaption());
            job.setPublishedAt(resolvedItem.getPublishedAt());
            job.setDurationSeconds(resolvedItem.getDurationSeconds());
            job.setThumbnailUrl(resolvedItem.getThumbnailUrl());
            job.setAvailability(resolvedItem.getAvailability());
            job.setPlaylistTitle(resolvedItem.getPlaylistTitle());
        } else if (resolution != null && resolution.getItems().size() > 1) {
            job.setPlaylistTitle(sourceRequest.getSourceUrl());
        }
        return job;
    }

    private ContentProvider resolveProvider(String requestedPlatform, String sourceUrl) {
        if (requestedPlatform == null || requestedPlatform.isBlank() || "AUTO".equalsIgnoreCase(requestedPlatform)) {
            return providerRegistry.detect(sourceUrl);
        }
        return providerRegistry.byPlatform(Platform.valueOf(requestedPlatform.toUpperCase(Locale.ROOT)));
    }

    private SourceType resolveSourceType(String requestedSourceType, String sourceUrl) {
        if (requestedSourceType != null && !requestedSourceType.isBlank()
                && !"AUTO".equalsIgnoreCase(requestedSourceType)) {
            return SourceType.valueOf(requestedSourceType.toUpperCase(Locale.ROOT));
        }

        String normalized = sourceUrl.toLowerCase(Locale.ROOT);
        if (normalized.contains("list=") || normalized.contains("/playlist")) {
            return SourceType.PLAYLIST;
        }
        if (normalized.contains("/channel/") || normalized.contains("/@")
                || normalized.contains("/user/") || normalized.contains("/c/")) {
            if (!normalized.contains("/video/") && !normalized.contains("/watch") && !normalized.contains("/reel/")
                    && !normalized.contains("/p/")) {
                return SourceType.PROFILE;
            }
        }
        return SourceType.DIRECT_URL;
    }

    private String requestedVariant(SourceRequest sourceRequest) {
        return defaultText(sourceRequest.getRequestedDownloadType(), "VIDEO") + "|"
                + defaultText(sourceRequest.getRequestedQuality(), "best") + "|"
                + defaultText(sourceRequest.getRequestedFormat(), "mp4");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record SubmissionResult(SourceRequest sourceRequest, Job primaryJob) {
    }
}
