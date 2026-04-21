package com.example.platform.downloader.ui.dto;

import com.example.platform.downloader.domain.RuntimeSettings;

public class RuntimeSettingsPayload {

    private String telegramBotToken;
    private String telegramChatId;
    private String googleDriveServiceAccountJson;
    private String googleDriveFolderId;
    private String baseUrl;

    public RuntimeSettings toRuntimeSettings() {
        RuntimeSettings runtimeSettings = new RuntimeSettings();
        runtimeSettings.setTelegramBotToken(telegramBotToken);
        runtimeSettings.setTelegramChatId(telegramChatId);
        runtimeSettings.setGoogleDriveServiceAccountJson(googleDriveServiceAccountJson);
        runtimeSettings.setGoogleDriveFolderId(googleDriveFolderId);
        runtimeSettings.setBaseUrl(baseUrl);
        return runtimeSettings;
    }

    public String getTelegramBotToken() {
        return telegramBotToken;
    }

    public void setTelegramBotToken(String telegramBotToken) {
        this.telegramBotToken = telegramBotToken;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getGoogleDriveServiceAccountJson() {
        return googleDriveServiceAccountJson;
    }

    public void setGoogleDriveServiceAccountJson(String googleDriveServiceAccountJson) {
        this.googleDriveServiceAccountJson = googleDriveServiceAccountJson;
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
