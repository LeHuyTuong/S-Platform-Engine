package com.example.platform.downloader.infrastructure;

import com.example.platform.downloader.domain.JobEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobEventRepository extends JpaRepository<JobEvent, Long> {
    List<JobEvent> findTop500ByJobIdOrderBySequenceNoDesc(String jobId);

    Optional<JobEvent> findTopByJobIdOrderBySequenceNoDesc(String jobId);

    @Modifying
    @Query(value = """
            delete from job_events
            where job_id = :jobId
              and id not in (
                select id from (
                  select id from job_events where job_id = :jobId order by sequence_no desc limit 500
                ) t
              )
            """, nativeQuery = true)
    void trimToLast500(@Param("jobId") String jobId);
}
