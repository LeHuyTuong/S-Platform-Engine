package com.example.platform.downloader.application.outbox;

import com.example.platform.downloader.application.job.DownloadWorkerService;
import com.example.platform.downloader.domain.entity.OutboxEvent;
import com.example.platform.downloader.domain.enums.OutboxStatus;
import com.example.platform.downloader.infrastructure.OutboxEventRepository;
import com.example.platform.downloader.infrastructure.WorkerProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
/**
 * Chuyển outbox row đã lưu bền thành công việc thực tế cho worker.
 *
 * Nếu tắt Redis thì `dispatchPending()` chỉ publish rồi đẩy sang thread pool local.
 * Nếu bật Redis thì row sẽ được đẩy vào stream để worker instance khác consume.
 */
public class OutboxDispatcher {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxService outboxService;
    private final DownloadWorkerService downloadWorkerService;
    private final WorkerProperties workerProperties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ExecutorService workerExecutor;

    public OutboxDispatcher(OutboxEventRepository outboxEventRepository,
                            OutboxService outboxService,
                            DownloadWorkerService downloadWorkerService,
                            WorkerProperties workerProperties,
                            ObjectMapper objectMapper,
                            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxService = outboxService;
        this.downloadWorkerService = downloadWorkerService;
        this.workerProperties = workerProperties;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        this.workerExecutor = Executors.newFixedThreadPool(Math.max(1, workerProperties.getConcurrency()));
    }

    @PostConstruct
    public void init() {
        if (workerProperties.isRedisEnabled() && stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForStream().createGroup(
                        workerProperties.getStreamKey(),
                        ReadOffset.latest(),
                        workerProperties.getConsumerGroup()
                );
            } catch (Exception ignored) {
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        workerExecutor.shutdown();
        try {
            workerExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Scheduled(fixedDelay = 2000L)
    public void dispatchPending() {
        if (!workerProperties.isEnabled()) {
            return;
        }

        List<OutboxEvent> pending = outboxEventRepository
                .findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxStatus.PENDING,
                        java.time.LocalDateTime.now()
                );

        for (OutboxEvent event : pending) {
            try {
                if (workerProperties.isRedisEnabled() && stringRedisTemplate != null) {
                    var recordId = stringRedisTemplate.opsForStream().add(MapRecord.create(
                            workerProperties.getStreamKey(),
                            Map.of("outboxEventId", event.getId())
                    ));
                    outboxService.markPublished(event, recordId != null ? recordId.getValue() : "n/a");
                    continue;
                }

                outboxService.markPublished(event, "local");
                workerExecutor.submit(() -> process(event.getId()));
            } catch (Exception e) {
                outboxService.markFailed(event, e.getMessage(), 30);
            }
        }
    }

    @Scheduled(fixedDelay = 2000L)
    public void consumeRedisStream() {
        if (!workerProperties.isEnabled() || !workerProperties.isRedisEnabled() || stringRedisTemplate == null) {
            return;
        }
        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                    Consumer.from(workerProperties.getConsumerGroup(), workerProperties.getWorkerId()),
                    StreamReadOptions.empty().count(10).block(Duration.ofMillis(250)),
                    StreamOffset.create(workerProperties.getStreamKey(), ReadOffset.lastConsumed())
            );
            if (records == null) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                String outboxEventId = String.valueOf(record.getValue().get("outboxEventId"));
                process(outboxEventId);
                stringRedisTemplate.opsForStream().acknowledge(
                        workerProperties.getStreamKey(),
                        workerProperties.getConsumerGroup(),
                        record.getId()
                );
            }
        } catch (Exception ignored) {
        }
    }

    @Scheduled(fixedDelay = 60000L)
    public void recoverExpiredJobs() {
        if (workerProperties.isEnabled()) {
            downloadWorkerService.recoverExpiredRunningJobs();
        }
    }

    private void process(String eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
        if (event == null || event.getStatus() == OutboxStatus.PROCESSED) {
            return;
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() {});
            if ("SOURCE_REQUEST_ACCEPTED".equals(event.getEventType())) {
                downloadWorkerService.handleSourceRequest(String.valueOf(payload.get("sourceRequestId")));
            } else if ("DOWNLOAD_JOB_QUEUED".equals(event.getEventType())) {
                downloadWorkerService.handleDownloadJob(String.valueOf(payload.get("jobId")));
            }
            outboxService.markProcessed(event);
        } catch (Exception e) {
            outboxService.markFailed(event, e.getMessage(), 30);
        }
    }
}
