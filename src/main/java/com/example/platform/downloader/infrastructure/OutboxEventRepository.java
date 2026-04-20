package com.example.platform.downloader.infrastructure;

import com.example.platform.downloader.domain.entity.OutboxEvent;
import com.example.platform.downloader.domain.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(OutboxStatus status, LocalDateTime availableAt);
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
    long countByStatus(OutboxStatus status);
}
