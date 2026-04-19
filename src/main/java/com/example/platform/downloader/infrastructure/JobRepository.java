package com.example.platform.downloader.infrastructure;

import com.example.platform.downloader.domain.Job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.platform.modules.user.domain.User;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {
    List<Job> findByUserOrderByCreatedAtDesc(User user);
    int countByUserAndCreatedAtAfter(User user, LocalDateTime date);
}
