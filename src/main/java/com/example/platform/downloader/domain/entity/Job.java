package com.example.platform.downloader.domain.entity;

import com.example.platform.downloader.domain.enums.FailureCategory;
import com.example.platform.downloader.domain.enums.JobState;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.domain.enums.SourceType;
import com.example.platform.kernel.domain.BaseAuditEntity;
import com.example.platform.modules.user.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jobs")
/**
 * Tác vụ tải cụ thể mà worker sẽ thực thi.
 *
 * Flow chính:
 * - `SourceRequest` sinh ra một hoặc nhiều `Job`
 * - worker claim `Job` qua DB lease
 * - `yt-dlp` chạy và cập nhật progress, log, metadata
 * - kết thúc ở `COMPLETED`, `FAILED`, `RETRY_WAIT` hoặc `BLOCKED`
 */
public class Job extends BaseAuditEntity {

    // Mã job ổn định, cũng được dùng làm tên thư mục lưu file.
    @Id
    private String id;

    // Quan hệ sở hữu và quan hệ ngược về request cha.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_request_id")
    private SourceRequest sourceRequest;

    // Nhận diện nguồn và trạng thái vòng đời hiện tại.
    @Column(nullable = false, length = 1000)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private JobState state;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private FailureCategory failureCategory;

    // Buffer log tạm thời, được hydrate từ `job_events` khi trả status API.
    @Transient
    private List<String> logs;

    // Tùy chọn đầu ra, khóa dedupe và các field hiển thị cho UI.
    private String outputFilename;
    private String playlistTitle;
    private String videoTitle;
    private Integer totalItems;
    private Integer currentItem;
    private String downloadType;
    private String quality;
    private String format;
    private String requestedVariant;
    private String externalItemId;
    private String proxy;
    private String proxyRef;
    private String startTime;
    private String endTime;
    private boolean cleanMetadata;
    private boolean writeThumbnail;
    private String watermarkText;
    private String titleTemplate;
    private String errorMessage;

    // Metadata lấy ra từ bước resolve hoặc từ output của `yt-dlp`.
    private String authorName;

    @Column(columnDefinition = "text")
    private String captionText;

    private LocalDateTime publishedAt;
    private Long durationSeconds;
    private String thumbnailUrl;
    private String availability;

    // Nhóm field phục vụ retry và cơ chế lease giữa nhiều worker.
    private int attemptCount;
    private int maxAttempts = 4;
    private LocalDateTime nextAttemptAt;
    private String leaseOwner;
    private LocalDateTime leaseExpiresAt;
    private LocalDateTime queuedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String downloadPath;

    // Progress runtime chỉ sống trong memory hoặc response, không persist xuống DB.
    @Transient
    private String downloadSpeed;

    @Transient
    private String eta;

    @Transient
    private double progressPercent;

    public Job() {
        this.id = UUID.randomUUID().toString();
        this.status = JobStatus.PENDING;
        this.state = JobState.ACCEPTED;
        this.logs = new ArrayList<>();
    }

    public Job(String url) {
        this();
        this.url = url;
    }

    public synchronized void addLog(String log) {
        if (this.logs == null) {
            this.logs = new ArrayList<>();
        }
        this.logs.add(log);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SourceRequest getSourceRequest() {
        return sourceRequest;
    }

    public void setSourceRequest(SourceRequest sourceRequest) {
        this.sourceRequest = sourceRequest;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public FailureCategory getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(FailureCategory failureCategory) {
        this.failureCategory = failureCategory;
    }

    public List<String> getLogs() {
        if (logs == null) {
            logs = new ArrayList<>();
        }
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public String getPlaylistTitle() {
        return playlistTitle;
    }

    public void setPlaylistTitle(String playlistTitle) {
        this.playlistTitle = playlistTitle;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Integer getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(Integer currentItem) {
        this.currentItem = currentItem;
    }

    public String getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(String downloadType) {
        this.downloadType = downloadType;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getRequestedVariant() {
        return requestedVariant;
    }

    public void setRequestedVariant(String requestedVariant) {
        this.requestedVariant = requestedVariant;
    }

    public String getExternalItemId() {
        return externalItemId;
    }

    public void setExternalItemId(String externalItemId) {
        this.externalItemId = externalItemId;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getProxyRef() {
        return proxyRef;
    }

    public void setProxyRef(String proxyRef) {
        this.proxyRef = proxyRef;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean isCleanMetadata() {
        return cleanMetadata;
    }

    public void setCleanMetadata(boolean cleanMetadata) {
        this.cleanMetadata = cleanMetadata;
    }

    public boolean isWriteThumbnail() {
        return writeThumbnail;
    }

    public void setWriteThumbnail(boolean writeThumbnail) {
        this.writeThumbnail = writeThumbnail;
    }

    public String getWatermarkText() {
        return watermarkText;
    }

    public void setWatermarkText(String watermarkText) {
        this.watermarkText = watermarkText;
    }

    public String getTitleTemplate() {
        return titleTemplate;
    }

    public void setTitleTemplate(String titleTemplate) {
        this.titleTemplate = titleTemplate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCaptionText() {
        return captionText;
    }

    public void setCaptionText(String captionText) {
        this.captionText = captionText;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getLeaseOwner() {
        return leaseOwner;
    }

    public void setLeaseOwner(String leaseOwner) {
        this.leaseOwner = leaseOwner;
    }

    public LocalDateTime getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(LocalDateTime leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(LocalDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }

    public String getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setDownloadSpeed(String downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public String getEta() {
        return eta;
    }

    public void setEta(String eta) {
        this.eta = eta;
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(double progressPercent) {
        this.progressPercent = progressPercent;
    }

    public enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
