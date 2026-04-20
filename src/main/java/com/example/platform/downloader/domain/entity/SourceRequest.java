package com.example.platform.downloader.domain.entity;

import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.domain.enums.SourceRequestState;
import com.example.platform.downloader.domain.enums.SourceType;
import com.example.platform.kernel.domain.BaseAuditEntity;
import com.example.platform.modules.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "source_requests")
/**
 * Bản ghi yêu cầu đầu vào ở tầng API, được tạo ra trước khi có job tải cụ thể.
 *
 * Một `SourceRequest` có thể sinh ra:
 * - đúng 1 `Job` nếu là `DIRECT_URL`
 * - nhiều job con sau bước resolve nếu là `PLAYLIST` hoặc `PROFILE`
 */
public class SourceRequest extends BaseAuditEntity {

    // Mã request ổn định để UI nhận ngay sau khi submit.
    @Id
    private String id;

    // Chủ sở hữu của request. Mọi job con đều kế thừa user này.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Thông tin định tuyến provider, được detect từ URL hoặc ép từ client.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SourceRequestState state;

    // URL gốc người dùng gửi lên và bản normalize dùng cho resolve hoặc dedupe.
    @Column(nullable = false, length = 1000)
    private String sourceUrl;

    @Column(length = 1000)
    private String normalizedUrl;

    // Tùy chọn đầu ra sẽ được copy sang job để worker không phụ thuộc web/session.
    @Column(length = 32)
    private String requestedDownloadType;

    @Column(length = 64)
    private String requestedQuality;

    @Column(length = 64)
    private String requestedFormat;

    @Column(length = 255)
    private String proxyRef;

    @Column(length = 500)
    private String proxy;

    @Column(length = 32)
    private String startTime;

    @Column(length = 32)
    private String endTime;

    // Các cờ hậu xử lý được áp dụng khi worker chạy job.
    @Column(nullable = false)
    private boolean cleanMetadata;

    @Column(nullable = false)
    private boolean writeThumbnail;

    @Column(length = 255)
    private String watermarkText;

    @Column(length = 255)
    private String titleTemplate;

    // Kết quả resolve được ghi bởi provider hoặc worker của pipeline.
    @Column(length = 2000)
    private String errorMessage;

    @Column(length = 1000)
    private String blockedReason;

    private Integer resolvedCount;

    public SourceRequest() {
        this.id = UUID.randomUUID().toString();
        this.state = SourceRequestState.ACCEPTED;
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

    public SourceRequestState getState() {
        return state;
    }

    public void setState(SourceRequestState state) {
        this.state = state;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public void setNormalizedUrl(String normalizedUrl) {
        this.normalizedUrl = normalizedUrl;
    }

    public String getRequestedDownloadType() {
        return requestedDownloadType;
    }

    public void setRequestedDownloadType(String requestedDownloadType) {
        this.requestedDownloadType = requestedDownloadType;
    }

    public String getRequestedQuality() {
        return requestedQuality;
    }

    public void setRequestedQuality(String requestedQuality) {
        this.requestedQuality = requestedQuality;
    }

    public String getRequestedFormat() {
        return requestedFormat;
    }

    public void setRequestedFormat(String requestedFormat) {
        this.requestedFormat = requestedFormat;
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

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
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

    public String getBlockedReason() {
        return blockedReason;
    }

    public void setBlockedReason(String blockedReason) {
        this.blockedReason = blockedReason;
    }

    public Integer getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(Integer resolvedCount) {
        this.resolvedCount = resolvedCount;
    }
}
