package com.example.platform.downloader.domain;

import com.example.platform.kernel.domain.BaseAuditEntity;
import com.example.platform.modules.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "provider_credentials")
/**
 * Lưu credential đã mã hóa theo user/provider/type.
 *
 * Ở v1 bảng này chủ yếu dùng cho cookies.txt để yt-dlp truy cập được nội dung
 * cần cookie trình duyệt.
 */
public class ProviderCredential extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Phạm vi sở hữu và provider áp dụng cho credential này.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CredentialType credentialType;

    // Payload đã mã hóa và IV dùng để giải mã AES-GCM.
    @Column(nullable = false, columnDefinition = "text")
    private String encryptedPayload;

    @Column(nullable = false, length = 255)
    private String iv;

    @Column(length = 255)
    private String fileName;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public CredentialType getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(CredentialType credentialType) {
        this.credentialType = credentialType;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(String encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
