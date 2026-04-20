package com.example.platform.downloader.domain.entity;

import com.example.platform.kernel.domain.BaseAuditEntity;
import com.example.platform.modules.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_connection_settings")
/**
 * Cấu hình kết nối theo từng user, được lưu bền thay vì nhét vào `HttpSession`.
 *
 * Các field nhạy cảm được lưu dưới dạng dữ liệu mã hóa kèm IV để worker nền vẫn dùng
 * được mà API không cần trả raw secret về client.
 */
public class UserConnectionSettings extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User sở hữu bộ cấu hình kết nối này.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Telegram bot token đã được mã hóa bằng master key của ứng dụng.
    @Column(columnDefinition = "text")
    private String encryptedTelegramBotToken;

    @Column(length = 255)
    private String telegramBotTokenIv;

    // Giá trị không phải secret nhưng cần lưu cùng Telegram token để gửi thông báo.
    @Column(length = 255)
    private String telegramChatId;

    // JSON service account của Google Drive đã được mã hóa.
    @Column(columnDefinition = "text")
    private String encryptedGoogleDriveServiceAccountJson;

    @Column(length = 255)
    private String googleDriveServiceAccountJsonIv;

    @Column(length = 255)
    private String googleDriveFolderId;

    // Base URL override theo user, dùng khi tạo link công khai trong thông báo.
    @Column(length = 512)
    private String baseUrl;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEncryptedTelegramBotToken() {
        return encryptedTelegramBotToken;
    }

    public void setEncryptedTelegramBotToken(String encryptedTelegramBotToken) {
        this.encryptedTelegramBotToken = encryptedTelegramBotToken;
    }

    public String getTelegramBotTokenIv() {
        return telegramBotTokenIv;
    }

    public void setTelegramBotTokenIv(String telegramBotTokenIv) {
        this.telegramBotTokenIv = telegramBotTokenIv;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getEncryptedGoogleDriveServiceAccountJson() {
        return encryptedGoogleDriveServiceAccountJson;
    }

    public void setEncryptedGoogleDriveServiceAccountJson(String encryptedGoogleDriveServiceAccountJson) {
        this.encryptedGoogleDriveServiceAccountJson = encryptedGoogleDriveServiceAccountJson;
    }

    public String getGoogleDriveServiceAccountJsonIv() {
        return googleDriveServiceAccountJsonIv;
    }

    public void setGoogleDriveServiceAccountJsonIv(String googleDriveServiceAccountJsonIv) {
        this.googleDriveServiceAccountJsonIv = googleDriveServiceAccountJsonIv;
    }

    public String getGoogleDriveFolderId() {
        return googleDriveFolderId;
    }

    public void setGoogleDriveFolderId(String googleDriveFolderId) {
        this.googleDriveFolderId = googleDriveFolderId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
