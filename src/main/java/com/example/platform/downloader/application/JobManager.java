package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.infrastructure.JobRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobManager {

    private final JobRepository jobRepository;
    private final JobEventService jobEventService;

    public JobManager(JobRepository jobRepository, JobEventService jobEventService) {
        this.jobRepository = jobRepository;
        this.jobEventService = jobEventService;
    }

    public Job getJob(String id) {
        Job job = jobRepository.findById(id).orElse(null);
        if (job != null) {
            job.setLogs(jobEventService.getRecentMessages(id));
        }
        return job;
    }

    public Object getUserLock(String userId) {
        return userId.intern();
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll().stream()
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .peek(job -> job.setLogs(jobEventService.getRecentMessages(job.getId())))
                .collect(Collectors.toList());
    }
}
