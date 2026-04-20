package com.example.platform.downloader.domain;

import com.example.platform.kernel.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "download_attempts")
/**
 * Lịch sử chạy từng lần của một Job.
 *
 * Mỗi lần worker claim và chạy lại job sẽ tạo một DownloadAttempt mới để có thể
 * theo dõi retry độc lập với trạng thái cuối cùng đang nằm trên bảng jobs.
 */
public class DownloadAttempt extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Job cha mà lần chạy này thuộc về.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    // Số thứ tự lần chạy, bắt đầu từ 1 và tăng dần khi retry.
    @Column(nullable = false)
    private int attemptNumber;

    // Khoảng thời gian thực tế của lần chạy này.
    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private boolean success;

    // Thông tin tiến trình/lỗi bổ sung nếu lần chạy không thành công.
    private Integer exitCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private FailureCategory failureCategory;

    @Column(length = 2000)
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public FailureCategory getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(FailureCategory failureCategory) {
        this.failureCategory = failureCategory;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
