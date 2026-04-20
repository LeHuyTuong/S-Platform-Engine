package com.example.platform.downloader.domain.entity;

import com.example.platform.downloader.domain.enums.EventLevel;
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

@Entity
@Table(name = "job_events")
/**
 * Log hoặc event được persist để UI poll trạng thái và để debug sau sự cố.
 *
 * Dù service có thể chỉ giữ phần log gần nhất, từng dòng vẫn có `sequenceNo`
 * để khôi phục đúng thứ tự phát sinh.
 */
public class JobEvent extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Job cha nhận dòng event hoặc log này.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    // Sequence tăng dần để ghép lại log đúng thứ tự worker phát sinh.
    @Column(nullable = false)
    private long sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventLevel level;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public EventLevel getLevel() {
        return level;
    }

    public void setLevel(EventLevel level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
