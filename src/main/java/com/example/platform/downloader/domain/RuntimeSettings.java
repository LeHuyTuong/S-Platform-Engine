package com.example.platform.downloader.domain;

import java.io.Serializable;

/**
 * Payload object cho API cấu hình kết nối người dùng.
 *
 * Quy tắc bảo mật:
 * - Không đánh dấu `@Entity`
 * - Không log raw secret
 * - Không trả raw value về FE; API status chỉ trả boolean `hasValue`
 * - Implement `Serializable` để tương thích payload serialization nếu cần
 */
public class RuntimeSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Token Telegram Bot, lấy từ `@BotFather`. Null hoặc blank nghĩa là tính năng tắt. */
    private String telegramBotToken;

    /** Chat ID người nhận thông báo Telegram. Nếu có giá trị này, nó sẽ override Chat ID của `User`. */
    private String telegramChatId;

    /** Nội dung JSON của Google Drive Service Account. Null hoặc blank nghĩa là tính năng tắt. */
    private String googleDriveServiceAccountJson;

    /** Folder ID trên Google Drive để upload file. Null hoặc blank nghĩa là bỏ qua upload. */
    private String googleDriveFolderId;

    /** Override `app.base-url` trong `application.yml` nếu deploy sau reverse proxy. */
    private String baseUrl;

    // Constructors

    public RuntimeSettings() {
    }

    // Validation

    /** Kiểm tra xem settings có hợp lệ không, tức là ít nhất một field có giá trị. */
    public boolean isValid() {
        return hasValue(telegramBotToken)
                || hasValue(telegramChatId)
                || hasValue(googleDriveServiceAccountJson)
                || hasValue(googleDriveFolderId)
                || hasValue(baseUrl);
    }

    /** Trả về `true` nếu field không null và không rỗng sau khi trim. */
    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    // Convenience checkers cho từng field, dùng trong controller để mask response.

    public boolean hasTelegramBotToken() {
        return hasValue(telegramBotToken);
    }

    public boolean hasTelegramChatId() {
        return hasValue(telegramChatId);
    }

    public boolean hasGoogleDriveServiceAccountJson() {
        return hasValue(googleDriveServiceAccountJson);
    }

    public boolean hasGoogleDriveFolderId() {
        return hasValue(googleDriveFolderId);
    }

    public boolean hasBaseUrl() {
        return hasValue(baseUrl);
    }

    // Getters và setters. Không ghi log ở đây.

    public String getTelegramBotToken() {
        return telegramBotToken;
    }

    public void setTelegramBotToken(String v) {
        this.telegramBotToken = v == null ? null : v.strip();
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String v) {
        this.telegramChatId = v == null ? null : v.strip();
    }

    public String getGoogleDriveServiceAccountJson() {
        return googleDriveServiceAccountJson;
    }

    public void setGoogleDriveServiceAccountJson(String v) {
        this.googleDriveServiceAccountJson = v == null ? null : v.strip();
    }

    public String getGoogleDriveFolderId() {
        return googleDriveFolderId;
    }

    public void setGoogleDriveFolderId(String v) {
        this.googleDriveFolderId = v == null ? null : v.strip();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String v) {
        this.baseUrl = v == null ? null : v.strip();
    }
}
