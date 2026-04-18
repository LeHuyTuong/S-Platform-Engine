package com.example.platform.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.platform.modules.user.domain.User;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Gửi thông báo Telegram khi job download hoàn thành hoặc thất bại.
 *
 * Cấu hình trong application.yml:
 *   app.telegram.bot-token: YOUR_BOT_TOKEN   (bắt buộc)
 *
 * User cần set telegram_chat_id trong profile của họ.
 * Lấy chat_id: nhắn tin /start cho @userinfobot trên Telegram.
 *
 * Giới hạn Telegram Bot API: file ≤ 2GB. Notification chỉ gửi text + link.
 */
@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private static final String TELEGRAM_API = "https://api.telegram.org/bot";

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    /**
     * Gửi thông báo khi job COMPLETED.
     * Bao gồm: tên video, số lượng file, link tải về (nếu có).
     *
     * @param job      Job đã hoàn thành
     * @param user     User sở hữu job
     * @param files    Danh sách file đã tải về (từ DownloaderService.listJobFiles)
     * @param baseUrl  Base URL của server (ví dụ: "https://your-domain.com") để tạo download link
     */
    public void notifyJobCompleted(Job job, User user, List<Map<String, String>> files, String baseUrl) {
        if (!isConfigured(user)) return;

        String title = job.getVideoTitle() != null ? job.getVideoTitle()
                : (job.getPlaylistTitle() != null ? job.getPlaylistTitle() : "Video");

        StringBuilder msg = new StringBuilder();
        msg.append("✅ *Download hoàn thành!*\n\n");
        msg.append("📹 *").append(escapeMarkdown(title)).append("*\n");

        if (job.getTotalItems() != null && job.getTotalItems() > 1) {
            msg.append("📋 Playlist: ").append(job.getTotalItems()).append(" video\n");
        }

        msg.append("🆔 Job ID: `").append(job.getId()).append("`\n\n");

        if (!files.isEmpty()) {
            msg.append("📁 *File tải về:*\n");
            int shown = 0;
            for (Map<String, String> f : files) {
                if (shown >= 10) {
                    msg.append("  \\.\\.\\. và ").append(files.size() - 10).append(" file khác\n");
                    break;
                }
                String sizeStr = formatSize(Long.parseLong(f.getOrDefault("size", "0")));
                String downloadUrl = baseUrl + "/downloader/files/" + job.getId() + "/" + f.get("name");
                msg.append("  • [").append(escapeMarkdown(f.get("name"))).append("](")
                   .append(downloadUrl).append(") \\(").append(sizeStr).append("\\)\n");
                shown++;
            }
        } else {
            msg.append("⚠️ Chưa có file nào được lưu\\.\n");
        }

        sendMessage(user.getTelegramChatId(), msg.toString());
    }

    /**
     * Gửi thông báo khi job FAILED.
     */
    public void notifyJobFailed(Job job, User user) {
        if (!isConfigured(user)) return;

        String title = job.getVideoTitle() != null ? job.getVideoTitle() : job.getUrl();
        String errorMsg = job.getErrorMessage() != null ? job.getErrorMessage() : "Lỗi không xác định";

        String msg = "❌ *Download thất bại\\!*\n\n" +
                "📹 " + escapeMarkdown(title) + "\n" +
                "🔴 Lỗi: `" + escapeMarkdown(errorMsg) + "`\n" +
                "🆔 Job ID: `" + job.getId() + "`";

        sendMessage(user.getTelegramChatId(), msg);
    }

    /**
     * Gửi message text về Telegram chat của user.
     * Sử dụng Telegram Bot API sendMessage với MarkdownV2 parse_mode.
     */
    private void sendMessage(String chatId, String text) {
        if (botToken == null || botToken.isBlank()) {
            log.warn("[Telegram] bot-token chưa được cấu hình — bỏ qua notification");
            return;
        }
        try {
            String apiUrl = TELEGRAM_API + botToken + "/sendMessage";
            String body = "{\"chat_id\":\"" + chatId + "\","
                    + "\"text\":\"" + text.replace("\"", "\\\"") + "\","
                    + "\"parse_mode\":\"MarkdownV2\","
                    + "\"disable_web_page_preview\":false}";

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                log.warn("[Telegram] Gửi thất bại, HTTP {}", code);
            } else {
                log.info("[Telegram] Đã gửi thông báo cho chat_id={}", chatId);
            }
        } catch (Exception e) {
            log.error("[Telegram] Lỗi khi gửi notification: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isConfigured(User user) {
        if (user == null || user.getTelegramChatId() == null || user.getTelegramChatId().isBlank()) {
            return false; // User chưa set Telegram chat_id
        }
        return true;
    }

    /** Escape các ký tự đặc biệt theo MarkdownV2 của Telegram */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
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
