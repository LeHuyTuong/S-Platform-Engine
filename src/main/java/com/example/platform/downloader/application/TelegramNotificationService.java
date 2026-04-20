package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.platform.modules.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private static final String TELEGRAM_API = "https://api.telegram.org/bot";

    private final UserConnectionSettingsService userConnectionSettingsService;
    private final String downloadDir;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelegramNotificationService(UserConnectionSettingsService userConnectionSettingsService,
                                       @Value("${app.downloader.output-dir:downloads}") String downloadDir,
                                       @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.userConnectionSettingsService = userConnectionSettingsService;
        this.downloadDir = downloadDir;
        this.baseUrl = baseUrl;
    }

    public void notifyJobCompleted(Job job, User user, List<Map<String, String>> files) {
        String botToken = resolveBotToken(user);
        String chatId = resolveChatId(user);
        if (!isConfigured(botToken, chatId)) {
            return;
        }

        String title = job.getVideoTitle() != null ? job.getVideoTitle()
                : (job.getPlaylistTitle() != null ? job.getPlaylistTitle() : "Video");
        String resolvedBaseUrl = userConnectionSettingsService.resolveBaseUrl(user, baseUrl);

        StringBuilder msg = new StringBuilder();
        msg.append("✅ *Download hoàn thành!*\n\n");
        msg.append("📹 *").append(escapeMarkdown(title)).append("*\n");
        msg.append("🆔 Job ID: `").append(job.getId()).append("`\n");
        if (job.getTotalItems() != null && job.getTotalItems() > 1) {
            msg.append("📋 Playlist: ").append(job.getTotalItems()).append(" video\n");
        }
        msg.append("\n");
        if (!files.isEmpty()) {
            msg.append("📁 *File tải về:*\n");
            int shown = 0;
            for (Map<String, String> file : files) {
                if (shown >= 10) {
                    msg.append("  \\.\\.\\. và ").append(files.size() - 10).append(" file khác\n");
                    break;
                }
                String downloadUrl = resolvedBaseUrl + "/downloader/files/" + job.getId() + "/" + file.get("name");
                msg.append("  • [").append(escapeMarkdown(file.get("name"))).append("](")
                        .append(downloadUrl).append(") \\(")
                        .append(escapeMarkdown(formatSize(Long.parseLong(file.getOrDefault("size", "0")))))
                        .append("\\)\n");
                shown++;
            }
        } else {
            msg.append("⚠️ Chưa có file nào được lưu\\.\n");
        }

        sendMessage(chatId, msg.toString(), botToken);
        trySendVideo(chatId, title, job, files, botToken);
    }

    public void notifyJobFailed(Job job, User user) {
        String botToken = resolveBotToken(user);
        String chatId = resolveChatId(user);
        if (!isConfigured(botToken, chatId)) {
            return;
        }
        String title = job.getVideoTitle() != null ? job.getVideoTitle() : job.getUrl();
        String errorMsg = job.getErrorMessage() != null ? job.getErrorMessage() : "Lỗi không xác định";
        String msg = "❌ *Download thất bại\\!*\n\n"
                + "📹 " + escapeMarkdown(title) + "\n"
                + "🔴 Lỗi: `" + escapeMarkdown(errorMsg) + "`\n"
                + "🆔 Job ID: `" + job.getId() + "`";
        sendMessage(chatId, msg, botToken);
    }

    private boolean isConfigured(String botToken, String chatId) {
        if (botToken == null || botToken.isBlank()) {
            return false;
        }
        if (chatId == null || chatId.isBlank()) {
            return false;
        }
        return true;
    }

    private String resolveBotToken(User user) {
        if (user == null) {
            return null;
        }
        return userConnectionSettingsService.resolveTelegramBotToken(user);
    }

    private String resolveChatId(User user) {
        if (user == null) {
            return null;
        }
        return userConnectionSettingsService.resolveTelegramChatId(user);
    }

    private void trySendVideo(String chatId, String title, Job job, List<Map<String, String>> files, String botToken) {
        try {
            Optional<Map<String, String>> videoFile = files.stream()
                    .filter(file -> {
                        String name = file.getOrDefault("name", "").toLowerCase();
                        return name.endsWith(".mp4") || name.endsWith(".mkv")
                                || name.endsWith(".webm") || name.endsWith(".mov");
                    })
                    .findFirst();
            if (videoFile.isEmpty()) {
                return;
            }

            Map<String, String> file = videoFile.get();
            long size = Long.parseLong(file.getOrDefault("size", "0"));
            if (size <= 0 || size >= 50L * 1024 * 1024) {
                return;
            }

            Path path = Paths.get(downloadDir, "jobs", job.getId(), file.get("path"));
            if (!path.toFile().exists()) {
                return;
            }
            String caption = "📹 *" + escapeMarkdown(title) + "*\nSize: " + escapeMarkdown(formatSize(size));
            sendVideo(chatId, path.toFile(), caption, botToken);
        } catch (Exception e) {
            log.error("[Telegram] Error while trying to send video: {}", e.getMessage());
        }
    }

    private void sendVideo(String chatId, File videoFile, String caption, String botToken) {
        String boundary = "---" + System.currentTimeMillis() + "---";
        String lineFeed = "\r\n";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(TELEGRAM_API + botToken + "/sendVideo").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                writePart(os, boundary, "chat_id", chatId, lineFeed);
                writePart(os, boundary, "caption", caption, lineFeed);
                writePart(os, boundary, "parse_mode", "MarkdownV2", lineFeed);

                os.write(("--" + boundary + lineFeed).getBytes(StandardCharsets.UTF_8));
                os.write(("Content-Disposition: form-data; name=\"video\"; filename=\"" + videoFile.getName() + "\"" + lineFeed).getBytes(StandardCharsets.UTF_8));
                os.write(("Content-Type: video/mp4" + lineFeed + lineFeed).getBytes(StandardCharsets.UTF_8));
                try (FileInputStream fis = new FileInputStream(videoFile)) {
                    fis.transferTo(os);
                }
                os.write(lineFeed.getBytes(StandardCharsets.UTF_8));
                os.write(("--" + boundary + "--" + lineFeed).getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                log.warn("[Telegram] Send video failed, HTTP {}", conn.getResponseCode());
            }
        } catch (Exception e) {
            log.error("[Telegram] Upload video failed: {}", e.getMessage());
        }
    }

    private void writePart(OutputStream os, String boundary, String name, String value, String lineFeed) throws Exception {
        os.write(("--" + boundary + lineFeed).getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"" + lineFeed + lineFeed)
                .getBytes(StandardCharsets.UTF_8));
        os.write((value + lineFeed).getBytes(StandardCharsets.UTF_8));
    }

    private void sendMessage(String chatId, String text, String botToken) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(TELEGRAM_API + botToken + "/sendMessage").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "MarkdownV2");
            body.put("disable_web_page_preview", false);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(body));
            }

            if (conn.getResponseCode() != 200) {
                log.warn("[Telegram] Send message failed, HTTP {}", conn.getResponseCode());
            }
        } catch (Exception e) {
            log.error("[Telegram] Send message failed: {}", e.getMessage());
        }
    }

    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
