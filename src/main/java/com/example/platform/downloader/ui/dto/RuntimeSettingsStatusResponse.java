package com.example.platform.downloader.ui.dto;

public class RuntimeSettingsStatusResponse {

    private boolean hasSettings;
    private boolean hasTelegramToken;
    private boolean hasTelegramChatId;
    private boolean hasGoogleDriveServiceAccount;
    private boolean hasGoogleDriveFolderId;
    private boolean hasBaseUrl;

    public boolean isHasSettings() {
        return hasSettings;
    }

    public void setHasSettings(boolean hasSettings) {
        this.hasSettings = hasSettings;
    }

    public boolean isHasTelegramToken() {
        return hasTelegramToken;
    }

    public void setHasTelegramToken(boolean hasTelegramToken) {
        this.hasTelegramToken = hasTelegramToken;
    }

    public boolean isHasTelegramChatId() {
        return hasTelegramChatId;
    }

    public void setHasTelegramChatId(boolean hasTelegramChatId) {
        this.hasTelegramChatId = hasTelegramChatId;
    }

    public boolean isHasGoogleDriveServiceAccount() {
        return hasGoogleDriveServiceAccount;
    }

    public void setHasGoogleDriveServiceAccount(boolean hasGoogleDriveServiceAccount) {
        this.hasGoogleDriveServiceAccount = hasGoogleDriveServiceAccount;
    }

    public boolean isHasGoogleDriveFolderId() {
        return hasGoogleDriveFolderId;
    }

    public void setHasGoogleDriveFolderId(boolean hasGoogleDriveFolderId) {
        this.hasGoogleDriveFolderId = hasGoogleDriveFolderId;
    }

    public boolean isHasBaseUrl() {
        return hasBaseUrl;
    }

    public void setHasBaseUrl(boolean hasBaseUrl) {
        this.hasBaseUrl = hasBaseUrl;
    }
}
