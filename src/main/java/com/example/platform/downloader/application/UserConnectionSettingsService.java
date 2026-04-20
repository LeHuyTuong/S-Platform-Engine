package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.RuntimeSettings;
import com.example.platform.downloader.domain.entity.UserConnectionSettings;
import com.example.platform.downloader.infrastructure.UserConnectionSettingsRepository;
import com.example.platform.modules.user.domain.User;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserConnectionSettingsService {

    private static final Logger log = LoggerFactory.getLogger(UserConnectionSettingsService.class);

    private static final int MAX_TOKEN_LENGTH = 512;
    private static final int MAX_JSON_LENGTH = 8192;
    private static final int MAX_FOLDER_ID_LEN = 256;
    private static final int MAX_BASE_URL_LEN = 512;

    private final UserConnectionSettingsRepository repository;
    private final SecretCryptoService cryptoService;

    public UserConnectionSettingsService(UserConnectionSettingsRepository repository,
                                         SecretCryptoService cryptoService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    @Transactional
    public void save(RuntimeSettings settings, User user) {
        validateToken(settings.getTelegramBotToken());
        validateJson(settings.getGoogleDriveServiceAccountJson());
        validateFolderId(settings.getGoogleDriveFolderId());
        validateBaseUrl(settings.getBaseUrl());

        UserConnectionSettings entity = repository.findByUserId(user.getId())
                .orElseGet(UserConnectionSettings::new);
        entity.setUser(user);
        entity.setTelegramChatId(blankToNull(settings.getTelegramChatId()));
        entity.setGoogleDriveFolderId(blankToNull(settings.getGoogleDriveFolderId()));
        entity.setBaseUrl(blankToNull(settings.getBaseUrl()));

        var telegram = cryptoService.encrypt(settings.getTelegramBotToken());
        entity.setEncryptedTelegramBotToken(telegram.payload());
        entity.setTelegramBotTokenIv(telegram.iv());

        var driveJson = cryptoService.encrypt(settings.getGoogleDriveServiceAccountJson());
        entity.setEncryptedGoogleDriveServiceAccountJson(driveJson.payload());
        entity.setGoogleDriveServiceAccountJsonIv(driveJson.iv());

        repository.save(entity);
        log.info("[UserConnectionSettings] Saved settings for userId={}", user.getId());
    }

    public Optional<UserConnectionSettings> find(User user) {
        return repository.findByUserId(user.getId());
    }

    public RuntimeSettings getMasked(User user) {
        RuntimeSettings result = new RuntimeSettings();
        repository.findByUserId(user.getId()).ifPresent(entity -> {
            result.setTelegramChatId(entity.getTelegramChatId());
            result.setGoogleDriveFolderId(entity.getGoogleDriveFolderId());
            result.setBaseUrl(entity.getBaseUrl());
            if (entity.getEncryptedTelegramBotToken() != null) {
                result.setTelegramBotToken("configured");
            }
            if (entity.getEncryptedGoogleDriveServiceAccountJson() != null) {
                result.setGoogleDriveServiceAccountJson("configured");
            }
        });
        return result;
    }

    public String resolveTelegramBotToken(User user) {
        return repository.findByUserId(user.getId())
                .map(entity -> cryptoService.decrypt(
                        entity.getEncryptedTelegramBotToken(),
                        entity.getTelegramBotTokenIv()
                ))
                .filter(token -> token != null && !token.isBlank())
                .orElse(null);
    }

    public String resolveTelegramChatId(User user) {
        return repository.findByUserId(user.getId())
                .map(UserConnectionSettings::getTelegramChatId)
                .filter(chatId -> chatId != null && !chatId.isBlank())
                .orElse(user.getTelegramChatId());
    }

    public String resolveBaseUrl(User user, String fallback) {
        return repository.findByUserId(user.getId())
                .map(UserConnectionSettings::getBaseUrl)
                .filter(url -> url != null && !url.isBlank())
                .orElse(fallback);
    }

    @Transactional
    public void clear(User user) {
        repository.findByUserId(user.getId()).ifPresent(repository::delete);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private void validateToken(String token) {
        if (token == null || token.isBlank()) return;
        if (token.length() > MAX_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Telegram token quá dài (max " + MAX_TOKEN_LENGTH + " ký tự)");
        }
        if (token.contains("\n") || token.contains("\r") || token.contains("\t")) {
            throw new IllegalArgumentException("Telegram token không được chứa ký tự xuống dòng hoặc tab");
        }
    }

    private void validateJson(String json) {
        if (json == null || json.isBlank()) return;
        if (json.length() > MAX_JSON_LENGTH) {
            throw new IllegalArgumentException("Service Account JSON quá lớn (max " + MAX_JSON_LENGTH + " ký tự)");
        }
    }

    private void validateFolderId(String folderId) {
        if (folderId == null || folderId.isBlank()) return;
        if (folderId.length() > MAX_FOLDER_ID_LEN) {
            throw new IllegalArgumentException("Google Drive Folder ID quá dài");
        }
        if (!folderId.matches("[A-Za-z0-9_\\-]+")) {
            throw new IllegalArgumentException("Google Drive Folder ID chứa ký tự không hợp lệ");
        }
    }

    private void validateBaseUrl(String url) {
        if (url == null || url.isBlank()) return;
        if (url.length() > MAX_BASE_URL_LEN) {
            throw new IllegalArgumentException("Base URL quá dài (max " + MAX_BASE_URL_LEN + " ký tự)");
        }
        if (!url.startsWith("https://") && !url.startsWith("http://localhost") && !url.startsWith("http://127.")) {
            throw new IllegalArgumentException("Base URL phải bắt đầu bằng https:// (hoặc http://localhost)");
        }
        if (url.contains("\n") || url.contains("\r")) {
            throw new IllegalArgumentException("Base URL không được chứa ký tự xuống dòng");
        }
    }
}
