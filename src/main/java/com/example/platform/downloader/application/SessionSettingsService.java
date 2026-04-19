package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.RuntimeSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Quản lý RuntimeSettings trong HttpSession của người dùng.
 *
 * Bảo mật:
 *   - Settings sống trong server-side session (không bao giờ ra client)
 *   - Session timeout = 30 phút (cấu hình trong application.yml)
 *   - KHÔNG log raw token — chỉ log boolean có/không
 *   - KHÔNG dùng database hoặc file
 *
 * Key dùng trong session: "RUNTIME_SETTINGS"
 */
@Service
public class SessionSettingsService {

    private static final Logger log = LoggerFactory.getLogger(SessionSettingsService.class);
    static final String SESSION_KEY = "RUNTIME_SETTINGS";

    // ─────────────────────────────────────────────────────────────────────────
    // Input validation constants
    // ─────────────────────────────────────────────────────────────────────────

    private static final int MAX_TOKEN_LENGTH    = 512;
    private static final int MAX_JSON_LENGTH     = 8192;
    private static final int MAX_FOLDER_ID_LEN   = 256;
    private static final int MAX_BASE_URL_LEN    = 512;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validate và lưu settings vào session.
     * Throw IllegalArgumentException nếu input vi phạm security rules.
     */
    public void save(RuntimeSettings settings, HttpSession session) {
        if (settings == null) {
            clear(session);
            return;
        }

        // Validate từng field trước khi lưu
        validateToken(settings.getTelegramBotToken());
        validateJson(settings.getGoogleDriveServiceAccountJson());
        validateFolderId(settings.getGoogleDriveFolderId());
        validateBaseUrl(settings.getBaseUrl());

        session.setAttribute(SESSION_KEY, settings);

        // Log chỉ boolean — KHÔNG log raw value
        log.info("[SessionSettings] Saved: hasTelegram={}, hasDriveJson={}, hasDriveFolderId={}, hasBaseUrl={}",
                settings.hasTelegramBotToken(),
                settings.hasGoogleDriveServiceAccountJson(),
                settings.hasGoogleDriveFolderId(),
                settings.hasBaseUrl());
    }

    /**
     * Lấy settings từ session hiện tại.
     * Trả về Optional.empty() nếu chưa có settings hoặc session đã expire.
     */
    public Optional<RuntimeSettings> get(HttpSession session) {
        if (session == null) return Optional.empty();
        try {
            Object obj = session.getAttribute(SESSION_KEY);
            if (obj instanceof RuntimeSettings rs) {
                return Optional.of(rs);
            }
        } catch (IllegalStateException e) {
            // Session invalidated
            log.debug("[SessionSettings] Session already invalidated");
        }
        return Optional.empty();
    }

    /**
     * Xóa settings khỏi session (logout / manual clear).
     */
    public void clear(HttpSession session) {
        if (session == null) return;
        try {
            session.removeAttribute(SESSION_KEY);
            log.info("[SessionSettings] Settings cleared from session");
        } catch (IllegalStateException e) {
            log.debug("[SessionSettings] Session already invalidated when clearing");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Token không được chứa ký tự newline/tab (injection prevention).
     */
    private void validateToken(String token) {
        if (token == null || token.isBlank()) return;
        if (token.length() > MAX_TOKEN_LENGTH)
            throw new IllegalArgumentException("Telegram token quá dài (max " + MAX_TOKEN_LENGTH + " ký tự)");
        if (token.contains("\n") || token.contains("\r") || token.contains("\t"))
            throw new IllegalArgumentException("Telegram token không được chứa ký tự xuống dòng hoặc tab");
    }

    /**
     * Service account JSON không được quá lớn.
     */
    private void validateJson(String json) {
        if (json == null || json.isBlank()) return;
        if (json.length() > MAX_JSON_LENGTH)
            throw new IllegalArgumentException("Service Account JSON quá lớn (max " + MAX_JSON_LENGTH + " ký tự)");
    }

    /**
     * Folder ID chỉ chứa ký tự alphanumeric và dấu gạch nối/dưới.
     */
    private void validateFolderId(String folderId) {
        if (folderId == null || folderId.isBlank()) return;
        if (folderId.length() > MAX_FOLDER_ID_LEN)
            throw new IllegalArgumentException("Google Drive Folder ID quá dài");
        if (!folderId.matches("[A-Za-z0-9_\\-]+"))
            throw new IllegalArgumentException("Google Drive Folder ID chứa ký tự không hợp lệ");
    }

    /**
     * Base URL phải bắt đầu bằng https:// hoặc http:// (localhost chấp nhận http).
     */
    private void validateBaseUrl(String url) {
        if (url == null || url.isBlank()) return;
        if (url.length() > MAX_BASE_URL_LEN)
            throw new IllegalArgumentException("Base URL quá dài (max " + MAX_BASE_URL_LEN + " ký tự)");
        if (!url.startsWith("https://") && !url.startsWith("http://localhost") && !url.startsWith("http://127."))
            throw new IllegalArgumentException("Base URL phải bắt đầu bằng https:// (hoặc http://localhost)");
        if (url.contains("\n") || url.contains("\r"))
            throw new IllegalArgumentException("Base URL không được chứa ký tự xuống dòng");
    }
}
