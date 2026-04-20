package com.example.platform.downloader.infrastructure;

import com.example.platform.downloader.domain.StoredAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoredAssetRepository extends JpaRepository<StoredAsset, Long> {
    List<StoredAsset> findByJobIdOrderByCreatedAtAsc(String jobId);
    void deleteByJobId(String jobId);
}
