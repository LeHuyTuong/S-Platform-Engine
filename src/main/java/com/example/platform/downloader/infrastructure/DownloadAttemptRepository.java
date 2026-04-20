package com.example.platform.downloader.infrastructure;

import com.example.platform.downloader.domain.DownloadAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DownloadAttemptRepository extends JpaRepository<DownloadAttempt, Long> {
    List<DownloadAttempt> findByJobIdOrderByAttemptNumberDesc(String jobId);
}
