package com.example.platform.downloader.infrastructure;

import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.domain.JobState;
import com.example.platform.modules.user.domain.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findByUserOrderByCreatedAtDesc(User user);

    List<Job> findBySourceRequestIdOrderByCreatedAtAsc(String sourceRequestId);

    int countByUserAndCreatedAtAfter(User user, LocalDateTime date);

    long countByStateIn(Collection<JobState> states);

    boolean existsByUserIdAndPlatformAndExternalItemIdAndRequestedVariant(
            Long userId, com.example.platform.downloader.domain.Platform platform, String externalItemId, String requestedVariant);

    @Query("""
            select j from Job j
            where j.state in :states
              and (j.nextAttemptAt is null or j.nextAttemptAt <= :now)
            order by j.createdAt asc
            """)
    List<Job> findReadyJobs(@Param("states") List<JobState> states, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("""
            update Job j
            set j.leaseOwner = :leaseOwner,
                j.leaseExpiresAt = :leaseExpiresAt,
                j.state = :runningState,
                j.status = :runningStatus,
                j.startedAt = coalesce(j.startedAt, :now)
            where j.id = :jobId
              and j.state in :claimableStates
              and (j.leaseExpiresAt is null or j.leaseExpiresAt < :now or j.leaseOwner = :leaseOwner)
            """)
    int acquireLease(
            @Param("jobId") String jobId,
            @Param("leaseOwner") String leaseOwner,
            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt,
            @Param("runningState") JobState runningState,
            @Param("runningStatus") Job.JobStatus runningStatus,
            @Param("claimableStates") List<JobState> claimableStates,
            @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("""
            update Job j
            set j.leaseOwner = null,
                j.leaseExpiresAt = null
            where j.id = :jobId
            """)
    int releaseLease(@Param("jobId") String jobId);

    @Query("""
            select j from Job j
            where j.state = :state
              and j.leaseExpiresAt is not null
              and j.leaseExpiresAt < :now
            """)
    List<Job> findExpiredRunningJobs(@Param("state") JobState state, @Param("now") LocalDateTime now);
}
