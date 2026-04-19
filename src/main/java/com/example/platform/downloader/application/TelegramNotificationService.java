package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.domain.RuntimeSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.platform.modules.user.domain.User;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;

/**
 * Gửi thông báo Telegram khi job download hoàn thành hoặc thất bại.
 *
 * Thay đổi so với phiên bản cũ:
 *   - KHÔNG đọc bot-token từ @Value / application.yml
 *   - Token lấy từ RuntimeSettings trong HttpSession (qua SessionSettingsService)
 *   - Nếu session chưa có token → skip silently (không throw, không crash)
 *   - KHÔNG log raw token bất kỳ đâu
 *
 * User cần set telegram_chat_id trong profile của họ.
 * Lấy chat_id: nhắn tin /start cho @userinfobot trên Telegram.
 */
@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private static final String TELEGRAM_API = "https://api.telegram.org/bot";

    private final SessionSettingsService sessionSettingsService;
    private final String downloadDir;

    public TelegramNotificationService(SessionSettingsService sessionSettingsService,
                                       @Value("${app.downloader.output-dir:downloads}") String downloadDir) {
        this.sessionSettingsService = sessionSettingsService;
        this.downloadDir = downloadDir;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public notification API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Gửi thông báo khi job COMPLETED.
     *
     * @param job      Job đã hoàn thành
     * @param user     User sở hữu job
     * @param files    Danh sách file đã tải về
     * @param baseUrl  Base URL để build download link
     * @param session  HttpSession hiện tại — dùng để lấy bot token
     */
    public void notifyJobCompleted(Job job, User user, List<Map<String, String>> files,
                                   String baseUrl, HttpSession session) {
        String botToken = resolveBotToken(session);
        if (!isConfigured(user, botToken)) return;

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

        String chatId = resolveChatId(user, session);
        sendMessage(chatId, msg.toString(), botToken);

        // --- NEW: Cố gắng gửi trực tiếp file Video nếu dung lượng < 50MB ---
        try {
            Optional<Map<String, String>> videoFileOpt = files.stream()
                    .filter(f -> {
                        String name = f.getOrDefault("name", "").toLowerCase();
                        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") || name.endsWith(".mov");
                    })
                    .findFirst();

            if (videoFileOpt.isPresent()) {
                Map<String, String> f = videoFileOpt.get();
                long size = Long.parseLong(f.getOrDefault("size", "0"));
                
                // Telegram Bot API limit: 50MB (52,428,800 bytes)
                if (size > 0 && size < 50 * 1024 * 1024) {
                    Path filePath = Paths.get(downloadDir, job.getId(), f.get("path"));
                    if (filePath.toFile().exists()) {
                        String caption = "📹 *" + escapeMarkdown(title) + "*\nSize: " + formatSize(size);
                        sendVideo(chatId, filePath.toFile(), caption, botToken);
                    }
                } else if (size >= 50 * 1024 * 1024) {
                    sendMessage(chatId, "ℹ️ _Video quá lớn (" + formatSize(size) + ") để gửi trực tiếp qua Telegram Bot API (giới hạn 50MB). Vui lòng tải qua link phía trên._", botToken);
                }
            }
        } catch (Exception e) {
            log.error("[Telegram] Lỗi khi cố gắng gửi video file: {}", e.getMessage());
        }
    }

    /**
     * Gửi thông báo khi job FAILED.
     *
     * @param session HttpSession hiện tại — dùng để lấy bot token
     */
    public void notifyJobFailed(Job job, User user, HttpSession session) {
        String botToken = resolveBotToken(session);
        if (!isConfigured(user, botToken)) return;

        String title = job.getVideoTitle() != null ? job.getVideoTitle() : job.getUrl();
        String errorMsg = job.getErrorMessage() != null ? job.getErrorMessage() : "Lỗi không xác định";

        String msg = "❌ *Download thất bại\\!*\n\n" +
                "📹 " + escapeMarkdown(title) + "\n" +
                "🔴 Lỗi: `" + escapeMarkdown(errorMsg) + "`\n" +
                "🆔 Job ID: `" + job.getId() + "`";

        sendMessage(resolveChatId(user, session), msg, botToken);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy botToken từ session. Trả null nếu không có settings hoặc token rỗng.
     * KHÔNG log raw token.
     */
    private String resolveBotToken(HttpSession session) {
        if (session == null) return null;
        return sessionSettingsService.get(session)
                .map(RuntimeSettings::getTelegramBotToken)
                .filter(t -> t != null && !t.isBlank())
                .orElse(null);
    }

    /**
     * Lấy Chat ID: ưu tiên từ session (để test nhanh), fallback về DB.
     * KHÔNG log raw ID.
     */
    private String resolveChatId(User user, HttpSession session) {
        // Ưu tiên session (RuntimeSettings)
        if (session != null) {
            String sessionChatId = sessionSettingsService.get(session)
                    .map(RuntimeSettings::getTelegramChatId)
                    .filter(t -> t != null && !t.isBlank())
                    .orElse(null);
            if (sessionChatId != null) return sessionChatId;
        }
        // Fallback về User profile (DB)
        return user != null ? user.getTelegramChatId() : null;
    }

    private boolean isConfigured(User user, String botToken) {
        if (botToken == null || botToken.isBlank()) {
            log.debug("[Telegram] Bot token chưa được cấu hình trong session — bỏ qua notification");
            return false;
        }
        return true; // resolveChatId sẽ quản lý việc có target hay không
    }

    /**
     * Gửi file video trực tiếp tới Telegram bằng multipart/form-data.
     * Giới hạn Bot API: 50MB.
     */
    private void sendVideo(String chatId, File videoFile, String caption, String botToken) {
        String boundary = "---" + System.currentTimeMillis() + "---";
        String LINE_FEED = "\r\n";

        try {
            String apiUrl = TELEGRAM_API + botToken + "/sendVideo";
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000); // Tăng timeout cho upload
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                // Chat ID part
                os.write(("--" + boundary + LINE_FEED).getBytes());
                os.write(("Content-Disposition: form-data; name=\"chat_id\"" + LINE_FEED + LINE_FEED).getBytes());
                os.write((chatId + LINE_FEED).getBytes());

                // Caption part
                os.write(("--" + boundary + LINE_FEED).getBytes());
                os.write(("Content-Disposition: form-data; name=\"caption\"" + LINE_FEED + LINE_FEED).getBytes());
                os.write((caption + LINE_FEED).getBytes());
                
                os.write(("--" + boundary + LINE_FEED).getBytes());
                os.write(("Content-Disposition: form-data; name=\"parse_mode\"" + LINE_FEED + LINE_FEED).getBytes());
                os.write(("MarkdownV2" + LINE_FEED).getBytes());

                // Video file part
                os.write(("--" + boundary + LINE_FEED).getBytes());
                os.write(("Content-Disposition: form-data; name=\"video\"; filename=\"" + videoFile.getName() + "\"" + LINE_FEED).getBytes());
                os.write(("Content-Type: video/mp4" + LINE_FEED + LINE_FEED).getBytes());

                try (FileInputStream fis = new FileInputStream(videoFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
                os.write(LINE_FEED.getBytes());

                // End of multipart
                os.write(("--" + boundary + "--" + LINE_FEED).getBytes());
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                log.warn("[Telegram] Gửi video thất bại, HTTP {}", code);
            } else {
                log.info("[Telegram] Đã gửi trực tiếp video ({}) cho chat_id={}", 
                        formatSize(videoFile.length()), chatId);
            }
        } catch (Exception e) {
            log.error("[Telegram] Lỗi khi upload video: {}", e.getMessage());
        }
    }

    /**
     * Gửi message text về Telegram chat của user.
     * botToken được truyền vào — KHÔNG lưu field instance.
     */
    private void sendMessage(String chatId, String text, String botToken) {
        if (chatId == null || chatId.isBlank()) {
            log.warn("[Telegram] Chat ID trống — không thể gửi tin nhắn.");
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
    // Format helpers
    // ─────────────────────────────────────────────────────────────────────────

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
