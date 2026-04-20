package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.EventLevel;
import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.domain.JobEvent;
import com.example.platform.downloader.infrastructure.JobEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class JobEventService {

    private final JobEventRepository jobEventRepository;

    public JobEventService(JobEventRepository jobEventRepository) {
        this.jobEventRepository = jobEventRepository;
    }

    @Transactional
    public void append(Job job, EventLevel level, String message) {
        if (job == null || message == null) {
            return;
        }
        long nextSequence = jobEventRepository.findTopByJobIdOrderBySequenceNoDesc(job.getId())
                .map(JobEvent::getSequenceNo)
                .orElse(0L) + 1;

        JobEvent event = new JobEvent();
        event.setJob(job);
        event.setSequenceNo(nextSequence);
        event.setLevel(level);
        event.setMessage(message);
        jobEventRepository.save(event);
        jobEventRepository.trimToLast500(job.getId());
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
}
