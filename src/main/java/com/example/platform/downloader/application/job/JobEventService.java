package com.example.platform.downloader.application.job;

import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.entity.JobEvent;
import com.example.platform.downloader.domain.enums.EventLevel;
import com.example.platform.downloader.infrastructure.JobEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class JobEventService {

    private final JobEventRepository jobEventRepository;
    private final ConcurrentHashMap<String, AtomicLong> sequenceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> trimCounters = new ConcurrentHashMap<>();

    public JobEventService(JobEventRepository jobEventRepository) {
        this.jobEventRepository = jobEventRepository;
    }

    @Transactional
    public void append(Job job, EventLevel level, String message) {
        if (job == null || message == null) {
            return;
        }
        AtomicLong sequence = sequenceCache.computeIfAbsent(job.getId(), this::loadSequenceCounter);
        long nextSequence = sequence.incrementAndGet();

        JobEvent event = new JobEvent();
        event.setJob(job);
        event.setSequenceNo(nextSequence);
        event.setLevel(level);
        event.setMessage(message);
        jobEventRepository.save(event);
        AtomicInteger trimCounter = trimCounters.computeIfAbsent(job.getId(), ignored -> new AtomicInteger());
        if (trimCounter.incrementAndGet() >= 25) {
            jobEventRepository.trimToLast500(job.getId());
            trimCounter.set(0);
        }
    }

    @Transactional
    public void appendInfo(Job job, String message) {
        append(job, EventLevel.INFO, message);
    }

    @Transactional
    public void appendWarn(Job job, String message) {
        append(job, EventLevel.WARN, message);
    }

    @Transactional
    public void appendError(Job job, String message) {
        append(job, EventLevel.ERROR, message);
    }

    public List<String> getRecentMessages(String jobId) {
        if (jobId == null) {
            return Collections.emptyList();
        }
        List<JobEvent> events = jobEventRepository.findTop500ByJobIdOrderBySequenceNoDesc(jobId);
        List<String> messages = new ArrayList<>(events.size());
        for (int i = events.size() - 1; i >= 0; i--) {
            messages.add(events.get(i).getMessage());
        }
        return messages;
    }

    public void clearJobSession(String jobId) {
        if (jobId == null) {
            return;
        }
        sequenceCache.remove(jobId);
        trimCounters.remove(jobId);
    }

    private AtomicLong loadSequenceCounter(String jobId) {
        long current = jobEventRepository.findTopByJobIdOrderBySequenceNoDesc(jobId)
                .map(JobEvent::getSequenceNo)
                .orElse(0L);
        return new AtomicLong(current);
    }
}

