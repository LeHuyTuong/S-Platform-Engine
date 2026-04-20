package com.example.platform.downloader.infrastructure;

import com.example.platform.downloader.domain.entity.SourceRequest;
import com.example.platform.modules.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SourceRequestRepository extends JpaRepository<SourceRequest, String> {
    List<SourceRequest> findByUserOrderByCreatedAtDesc(User user);
}
