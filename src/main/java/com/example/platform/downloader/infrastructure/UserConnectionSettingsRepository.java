package com.example.platform.downloader.infrastructure;

import com.example.platform.downloader.domain.entity.UserConnectionSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserConnectionSettingsRepository extends JpaRepository<UserConnectionSettings, Long> {
    Optional<UserConnectionSettings> findByUserId(Long userId);
}
