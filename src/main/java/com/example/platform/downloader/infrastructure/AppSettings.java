package com.example.platform.downloader.infrastructure;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cấu hình toàn hệ thống - thay đổi nóng không cần restart Docker.
 * Admin có thể chỉnh từ giao diện /admin khi YouTube bắt đầu chặn IP.
 */
@Component
public class AppSettings {

    private final String settingsPath;
    private final ObjectMapper mapper = new ObjectMapper();

    public AppSettings(@Value("${app.downloader.output-dir:downloads}") String downloadDir) {
        this.settingsPath = downloadDir + "/settings.json";
    }

    @PostConstruct
    public void init() {
        load();
    }

    // Số luồng tải song song (1 = chậm nhưng an toàn nhất)
    private final AtomicInteger concurrentFragments = new AtomicInteger(1);

    // Thời gian nghỉ giữa các video trong playlist (giây)
    private final AtomicInteger sleepInterval = new AtomicInteger(15);

    // Thời gian nghỉ giữa các API request nội bộ (giây)
    private final AtomicInteger sleepRequests = new AtomicInteger(2);

    // Số lần thử lại khi lỗi mạng
    private final AtomicInteger retries = new AtomicInteger(10);

    // Dung lượng tối đa cho phép tải (MB, 0 = vô hạn)
    private final AtomicLong maxFileSizeMb = new AtomicLong(0);

    // Telegram & Drive Settings
    private String telegramBotToken = "";
    private String telegramChatId = "";
    private String googleDriveFolderId = "";
    private String baseUrl = "http://localhost:8080";

    // Getters
    public int getConcurrentFragments() { return concurrentFragments.get(); }
    public int getSleepInterval() { return sleepInterval.get(); }
    public int getSleepRequests() { return sleepRequests.get(); }
    public int getRetries() { return retries.get(); }
    public long getMaxFileSizeMb() { return maxFileSizeMb.get(); }

    // Setters (thread-safe, có thể gọi từ bất kỳ thread nào)
    public void setConcurrentFragments(int v) { concurrentFragments.set(v); save(); }
    public void setSleepInterval(int v) { sleepInterval.set(v); save(); }
    public void setSleepRequests(int v) { sleepRequests.set(v); save(); }
    public void setRetries(int v) { retries.set(v); save(); }
    public void setMaxFileSizeMb(long v) { maxFileSizeMb.set(v); save(); }

    public String getTelegramBotToken() { return telegramBotToken; }
    public void setTelegramBotToken(String v) { this.telegramBotToken = v; save(); }

    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String v) { this.telegramChatId = v; save(); }

    public String getGoogleDriveFolderId() { return googleDriveFolderId; }
    public void setGoogleDriveFolderId(String v) { this.googleDriveFolderId = v; save(); }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String v) { this.baseUrl = v; save(); }

    private synchronized void save() {
        try {
            File file = new File(settingsPath);
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            Map<String, Object> data = Map.of(
                "concurrentFragments", concurrentFragments.get(),
                "sleepInterval", sleepInterval.get(),
                "sleepRequests", sleepRequests.get(),
                "retries", retries.get(),
                "maxFileSizeMb", maxFileSizeMb.get(),
                "telegramBotToken", telegramBotToken,
                "telegramChatId", telegramChatId,
                "googleDriveFolderId", googleDriveFolderId,
                "baseUrl", baseUrl
            );
            mapper.writeValue(file, data);
            System.out.println(">>> [AppSettings] Đã lưu cấu hình vào: " + settingsPath);
        } catch (Exception e) {
            System.err.println(">>> [AppSettings] Lỗi khi lưu cấu hình: " + e.getMessage());
        }
    }

    private synchronized void load() {
        try {
            File file = new File(settingsPath);
            if (file.exists()) {
                Map<String, Object> data = mapper.readValue(file, Map.class);
                System.out.println(">>> [AppSettings] Đang tải cấu hình từ: " + settingsPath);
                
                if (data.containsKey("concurrentFragments")) concurrentFragments.set(((Number) data.get("concurrentFragments")).intValue());
                if (data.containsKey("sleepInterval")) sleepInterval.set(((Number) data.get("sleepInterval")).intValue());
                if (data.containsKey("sleepRequests")) sleepRequests.set(((Number) data.get("sleepRequests")).intValue());
                if (data.containsKey("retries")) retries.set(((Number) data.get("retries")).intValue());
                if (data.containsKey("maxFileSizeMb")) maxFileSizeMb.set(((Number) data.get("maxFileSizeMb")).longValue());
                if (data.containsKey("telegramBotToken")) telegramBotToken = (String) data.get("telegramBotToken");
                if (data.containsKey("telegramChatId")) telegramChatId = (String) data.get("telegramChatId");
                if (data.containsKey("googleDriveFolderId")) googleDriveFolderId = (String) data.get("googleDriveFolderId");
                if (data.containsKey("baseUrl")) baseUrl = (String) data.get("baseUrl");
                
                System.out.println(">>> [AppSettings] Tải cấu hình thành công: " + concurrentFragments.get() + "/" + sleepInterval.get());
            } else {
                System.out.println(">>> [AppSettings] Không tìm thấy file cấu hình, dùng mặc định: " + settingsPath);
            }
        } catch (Exception e) {
            System.err.println(">>> [AppSettings] Lỗi khi tải cấu hình: " + e.getMessage());
        }
    }
}
