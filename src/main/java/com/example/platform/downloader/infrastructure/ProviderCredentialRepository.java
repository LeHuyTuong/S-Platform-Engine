package com.example.platform.downloader.infrastructure;

import com.example.platform.downloader.domain.enums.CredentialType;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.domain.entity.ProviderCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderCredentialRepository extends JpaRepository<ProviderCredential, Long> {
    Optional<ProviderCredential> findByUserIdAndPlatformAndCredentialType(Long userId, Platform platform, CredentialType credentialType);
    void deleteByUserIdAndPlatformAndCredentialType(Long userId, Platform platform, CredentialType credentialType);
}
