package com.example.platform.downloader.ui.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SourceRequestResponse {

    private String id;
    private String platform;
    private String sourceType;
    private String state;
    private String sourceUrl;
    private Integer resolvedCount;
    private String errorMessage;
    private String blockedReason;
    private LocalDateTime createdAt;
    private List<JobStatusResponse> jobs = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Integer getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(Integer resolvedCount) {
        this.resolvedCount = resolvedCount;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<JobStatusResponse> getJobs() {
        return jobs;
    }

    public void setJobs(List<JobStatusResponse> jobs) {
        this.jobs = jobs;
    }
}
