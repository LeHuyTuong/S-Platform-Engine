package com.example.platform.downloader;

import org.springframework.stereotype.Service;
import java.util.concurrent.*;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class JobManager {

    // Concurrency limit: 2 downloads at a time
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, Job> activeJobs = new ConcurrentHashMap<>();
    
    private final JobRepository jobRepository;
    
    @Autowired
    public JobManager(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public Job submitJob(String url, com.example.platform.modules.user.domain.User currentUser, Consumer<Job> taskExecutor) {
        Job job = new Job(url);
        job.setUser(currentUser);
        jobRepository.save(job);
        activeJobs.put(job.getId(), job);

        executor.submit(() -> {
            job.setStatus(Job.JobStatus.RUNNING);
            jobRepository.save(job);
            try {
                taskExecutor.accept(job);
                job.setStatus(Job.JobStatus.COMPLETED);
                jobRepository.save(job);
            } catch (Exception e) {
                job.setStatus(Job.JobStatus.FAILED);
                job.addLog("Error: " + e.getMessage());
                jobRepository.save(job);
            } finally {
                // Optionally remove from activeJobs after a delay, or keep it.
            }
        });

        return job;
    }

    public Job getJob(String id) {
        // First try active memory (has logs)
        if (activeJobs.containsKey(id)) {
            return activeJobs.get(id);
        }
        // Fallback to database
        return jobRepository.findById(id).orElse(null);
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll().stream()
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
}
