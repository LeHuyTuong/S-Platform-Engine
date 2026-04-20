package com.example.platform.downloader.domain.entity;

import com.example.platform.downloader.domain.enums.OutboxStatus;
import com.example.platform.kernel.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
/**
 * Cầu nối bền vững giữa transaction DB và xử lý bất đồng bộ của worker.
 *
 * API hoặc worker ghi row vào đây trong cùng transaction với thay đổi domain,
 * sau đó dispatcher mới publish hoặc execute event để không bị mất ý định xử lý.
 */
public class OutboxEvent extends BaseAuditEntity {

    @Id
    private String id;

    // Metadata của aggregate để trace và replay khi cần.
    @Column(nullable = false, length = 64)
    private String aggregateType;

    @Column(nullable = false, length = 64)
    private String aggregateId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    // Trạng thái và metadata phụ trợ cho publish local hoặc qua Redis Streams.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime availableAt;

    private LocalDateTime publishedAt;

    private LocalDateTime processedAt;

    @Column(length = 2000)
    private String lastError;

    @Column(length = 128)
    private String streamMessageId;

    public OutboxEvent() {
        this.id = UUID.randomUUID().toString();
        this.status = OutboxStatus.PENDING;
        this.availableAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(LocalDateTime availableAt) {
        this.availableAt = availableAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getStreamMessageId() {
        return streamMessageId;
    }

    public void setStreamMessageId(String streamMessageId) {
        this.streamMessageId = streamMessageId;
    }
}
