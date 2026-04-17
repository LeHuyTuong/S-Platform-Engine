package com.example.platform.downloader;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Transient;
import jakarta.persistence.Column;

import com.example.platform.modules.user.domain.User;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 1000)
    private String url;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Transient
    private List<String> logs;

    private LocalDateTime createdAt;
    private String outputFilename;

    // Playlist tracking
    private String playlistTitle;
    private Integer totalItems;
    private Integer currentItem;
    // Download Preferences
    private String downloadType; // "VIDEO" or "AUDIO"
    private String quality; // "best", "1080", "720", "480"
    private String format; // "mp4", "mkv", "mp3"
    
    // MMO Features
    private String proxy; // HTTP(s) or SOCKS5 proxy
    private String startTime; // Clip start time HH:MM:SS
    private String endTime; // Clip end time HH:MM:SS
    
    private String errorMessage;

    public Job() {
        // Default constructor for serialization
    }

    public Job(String url) {
        this.id = UUID.randomUUID().toString();
        this.url = url;
        this.status = JobStatus.PENDING;
        this.logs = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
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

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public enum JobStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }
}
