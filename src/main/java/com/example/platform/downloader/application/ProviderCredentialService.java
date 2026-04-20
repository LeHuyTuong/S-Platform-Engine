package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.CredentialType;
import com.example.platform.downloader.domain.Platform;
import com.example.platform.downloader.domain.ProviderCredential;
import com.example.platform.downloader.infrastructure.ProviderCredentialRepository;
import com.example.platform.modules.user.domain.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProviderCredentialService {

    private final ProviderCredentialRepository providerCredentialRepository;
    private final SecretCryptoService cryptoService;

    public ProviderCredentialService(ProviderCredentialRepository providerCredentialRepository,
                                     SecretCryptoService cryptoService) {
        this.providerCredentialRepository = providerCredentialRepository;
        this.cryptoService = cryptoService;
    }

    @Transactional
    public void saveCookie(User user, Platform platform, String fileName, String content) {
        var encrypted = cryptoService.encrypt(content);
        ProviderCredential credential = providerCredentialRepository
                .findByUserIdAndPlatformAndCredentialType(user.getId(), platform, CredentialType.COOKIE)
                .orElseGet(ProviderCredential::new);
        credential.setUser(user);
        credential.setPlatform(platform);
        credential.setCredentialType(CredentialType.COOKIE);
        credential.setEncryptedPayload(encrypted.payload());
        credential.setIv(encrypted.iv());
        credential.setFileName(fileName);
        providerCredentialRepository.save(credential);
    }

    public boolean hasCookie(User user, Platform platform) {
        return providerCredentialRepository
                .findByUserIdAndPlatformAndCredentialType(user.getId(), platform, CredentialType.COOKIE)
                .isPresent();
    }

    public Optional<String> loadCookie(User user, Platform platform) {
        return providerCredentialRepository
                .findByUserIdAndPlatformAndCredentialType(user.getId(), platform, CredentialType.COOKIE)
                .map(credential -> cryptoService.decrypt(credential.getEncryptedPayload(), credential.getIv()));
    }

    @Transactional
    public void deleteCookie(User user, Platform platform) {
        providerCredentialRepository.deleteByUserIdAndPlatformAndCredentialType(
                user.getId(), platform, CredentialType.COOKIE
        );
    }
}
