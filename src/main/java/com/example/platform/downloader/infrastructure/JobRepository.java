package com.example.platform.downloader.infrastructure;

import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.JobState;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.modules.user.domain.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("select j from Job j left join fetch j.user order by j.createdAt desc")
    List<Job> findAllByOrderByCreatedAtDesc();

    List<Job> findByUserOrderByCreatedAtDesc(User user);

    @Query("""
            select j from Job j
            left join fetch j.sourceRequest
            where (:userId is null or j.user.id = :userId)
              and (:state is null or j.state = :state)
              and (:status is null or j.status = :status)
              and (:platform is null or j.platform = :platform)
            order by j.createdAt desc
            """,
            countQuery = """
            select count(j) from Job j
            where (:userId is null or j.user.id = :userId)
              and (:state is null or j.state = :state)
              and (:status is null or j.status = :status)
              and (:platform is null or j.platform = :platform)
            """)
    Page<Job> searchJobs(@Param("userId") Long userId,
                         @Param("state") JobState state,
                         @Param("status") Job.JobStatus status,
                         @Param("platform") Platform platform,
                         Pageable pageable);

    @Query("""
            select j from Job j
            left join fetch j.sourceRequest
            where j.sourceRequest.id in :sourceRequestIds
            order by j.sourceRequest.id asc, j.createdAt asc
            """)
    List<Job> findBySourceRequestIdInOrderBySourceRequestIdAscCreatedAtAsc(
            @Param("sourceRequestIds") Collection<String> sourceRequestIds);

    List<Job> findBySourceRequestIdOrderByCreatedAtAsc(String sourceRequestId);

    int countByUserAndCreatedAtAfter(User user, LocalDateTime date);

    long countByStateIn(Collection<JobState> states);

    boolean existsByUserIdAndPlatformAndExternalItemIdAndRequestedVariant(
            Long userId, Platform platform, String externalItemId, String requestedVariant);

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

    @Transactional
    @Modifying
    @Query("""
            update Job j
            set j.progressPercent = :progressPercent,
                j.downloadSpeed = :downloadSpeed,
                j.eta = :eta,
                j.playlistTitle = coalesce(:playlistTitle, j.playlistTitle),
                j.videoTitle = coalesce(:videoTitle, j.videoTitle),
                j.currentItem = coalesce(:currentItem, j.currentItem),
                j.totalItems = coalesce(:totalItems, j.totalItems),
                j.errorMessage = coalesce(:errorMessage, j.errorMessage)
            where j.id = :jobId
            """)
    int updateRuntimeProgress(
            @Param("jobId") String jobId,
            @Param("progressPercent") double progressPercent,
            @Param("downloadSpeed") String downloadSpeed,
            @Param("eta") String eta,
            @Param("playlistTitle") String playlistTitle,
            @Param("videoTitle") String videoTitle,
            @Param("currentItem") Integer currentItem,
            @Param("totalItems") Integer totalItems,
            @Param("errorMessage") String errorMessage);
}
