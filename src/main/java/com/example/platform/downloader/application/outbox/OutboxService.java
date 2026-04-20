package com.example.platform.downloader.application.outbox;

import com.example.platform.downloader.domain.entity.OutboxEvent;
import com.example.platform.downloader.domain.enums.OutboxStatus;
import com.example.platform.downloader.infrastructure.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxEvent create(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload) {
        return create(aggregateType, aggregateId, eventType, payload, 0);
    }

    @Transactional
    public OutboxEvent create(String aggregateType, String aggregateId, String eventType,
                              Map<String, Object> payload, int delaySeconds) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setAvailableAt(LocalDateTime.now().plusSeconds(Math.max(delaySeconds, 0)));
        try {
            event.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize outbox payload", e);
        }
        return outboxEventRepository.save(event);
    }

    @Transactional
    public void markPublished(OutboxEvent event, String streamMessageId) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        event.setStreamMessageId(streamMessageId);
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markProcessed(OutboxEvent event) {
        event.setStatus(OutboxStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markFailed(OutboxEvent event, String error, int delaySeconds) {
        event.setAttempts(event.getAttempts() + 1);
        event.setLastError(error);
        event.setAvailableAt(LocalDateTime.now().plusSeconds(delaySeconds));
        event.setStatus(OutboxStatus.PENDING);
        outboxEventRepository.save(event);
    }
}

