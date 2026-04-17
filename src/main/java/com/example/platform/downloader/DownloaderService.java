package com.example.platform.downloader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class DownloaderService {

    private final JobManager jobManager;
    private final String downloadDir;
    private final String cookieFile;
    private final AppSettings appSettings;

    public DownloaderService(JobManager jobManager,
            @Value("${app.downloader.output-dir:downloads}") String downloadDir,
            AppSettings appSettings) {
        this.jobManager = jobManager;
        this.downloadDir = downloadDir;
        this.cookieFile = downloadDir + "/cookies.txt";
        this.appSettings = appSettings;
    }

    public Job submitDownload(java.util.Map<String, String> payload, com.example.platform.modules.user.domain.User currentUser) {
        String url = payload.get("url");
        return jobManager.submitJob(url, currentUser, job -> {
            job.setDownloadType(payload.getOrDefault("type", "VIDEO"));
            job.setQuality(payload.getOrDefault("quality", "best"));
            job.setFormat(payload.getOrDefault("format", "mp4"));
            job.setProxy(payload.get("proxy"));
            job.setStartTime(payload.get("startTime"));
            job.setEndTime(payload.get("endTime"));
            executeDownload(job);
        });
    }

    public boolean hasCookieFile() {
        return new File(cookieFile).exists();
    }

    public void saveCookieFile(MultipartFile file) throws Exception {
        Path outputDataDir = Paths.get(downloadDir);
        if (!Files.exists(outputDataDir)) {
            Files.createDirectories(outputDataDir);
        }
        file.transferTo(new File(cookieFile));
    }

    public void deleteCookieFile() {
        new File(cookieFile).delete();
    }

    // Logic thực thi quá trình tải xuống
    public void executeDownload(Job job) {
        try {
            Path outputDataDir = Paths.get(downloadDir);
            if (!Files.exists(outputDataDir)) {
                Files.createDirectories(outputDataDir);
            }

            // Xoá các file tạm cũ trước khi bắt đầu tải
            cleanUpStaleFiles(outputDataDir);

            // File lưu trữ lịch sử tải để tránh tải lại (download-archive)
            String archiveFile = downloadDir + "/downloaded.txt";

            List<String> command = new ArrayList<>();
            command.add("yt-dlp");

            // Dựng lệnh định dạng (Format) dựa trên Tùy chọn của người dùng
            if ("AUDIO".equalsIgnoreCase(job.getDownloadType())) {
                command.add("-f");
                command.add("bestaudio");
                command.add("--extract-audio");
                command.add("--audio-format");
                command.add(job.getFormat() != null ? job.getFormat() : "mp3");
                command.add("--audio-quality");
                command.add("0"); // 0 là chất lượng tốt nhất
            } else {
                // Cấu hình tải VIDEO
                command.add("-f");
                String resolution = job.getQuality();
                if (resolution == null || "best".equalsIgnoreCase(resolution)) {
                    command.add("bv*+ba/b");
                } else {
                    // Thử độ phân giải chính xác (ví dụ 1080) hoặc dùng cái tốt nhất nếu không có
                    command.add("bv*[height<=" + resolution + "]+ba/b");
                }

                String ext = job.getFormat();
                if (ext != null) {
                    command.add("--merge-output-format");
                    command.add(ext);
                }
            }

            // Metadata, subtitle, and thumbnail configurations for MMO SEO needs
            command.add("--embed-metadata");
            command.add("--write-description");
            command.add("--write-thumbnail");
            command.add("--convert-thumbnails");
            command.add("jpg");
            
            command.add("--embed-subs");
            command.add("--sub-langs");
            command.add("vi,en.*");
            
            // Cut specific time sections if provided
            if (job.getStartTime() != null && !job.getStartTime().isEmpty() &&
                job.getEndTime() != null && !job.getEndTime().isEmpty()) {
                command.add("--download-sections");
                command.add("*" + job.getStartTime() + "-" + job.getEndTime());
            }

            // MMO Proxy support
            if (job.getProxy() != null && !job.getProxy().isEmpty()) {
                command.add("--proxy");
                command.add(job.getProxy());
            }

            // Tránh tải lại video đã có trong lịch sử archive
            command.add("--download-archive");
            command.add(archiveFile);

            // Tiếp tục tải (Resume) phần đang tải dở và cấu hình an toàn
            command.add("--continue");
            command.add("--no-overwrites");

            // Khuôn mẫu tên file xuất ra (hỗ trợ playlist)
            command.add("-o");
            command.add(downloadDir + "/%(playlist)s/%(playlist_index)03d - %(title).200B [%(id)s].%(ext)s");

            // Cấu hình hiệu năng & chống chặn - đọc từ AppSettings (có thể thay đổi nóng từ Admin)
            command.add("--concurrent-fragments");
            command.add(String.valueOf(appSettings.getConcurrentFragments()));
            command.add("--sleep-interval");
            command.add(String.valueOf(appSettings.getSleepInterval()));
            command.add("--retries");
            command.add(String.valueOf(appSettings.getRetries()));
            command.add("--retry-sleep");
            command.add("5");

            // Dùng Cookies (nếu đã upload) để bẻ khoá nội dung giới hạn
            if (hasCookieFile()) {
                command.add("--cookies");
                command.add(cookieFile);
                job.addLog("Using cookies file: " + cookieFile);
            }

            // In mỗi tiến trình trên một dòng mới để parse log dễ dàng
            command.add("--newline");

            // Chống ban (Anti-Ban): Ép dùng IPv4 (dải IP IPv6 thường bị hạn chế/chặn rất gắt)
            command.add("--force-ipv4");

            // Chống ban: Giả lập trình duyệt Chrome thật
            command.add("--user-agent");
            command.add(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // Chống ban: Thời gian chờ giữa các lệnh API ngầm
            command.add("--sleep-requests");
            command.add(String.valueOf(appSettings.getSleepRequests()));

            // Gỡ lỗi (Hiển thị info nhiều hơn)
            command.add("--verbose");

            // URL
            command.add(job.getUrl());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    job.addLog(line);

                    // Phân tích tên playlist: ví dụ "[download] Finished downloading playlist: TÊN_PLAYLIST"
                    if (line.contains("Finished downloading playlist: ")) {
                        String title = line.substring(line.indexOf("Finished downloading playlist: ") + 32);
                        job.setPlaylistTitle(title);
                    }

                    // Phân tích quá trình tiến độ (Progress): ví dụ "[download] Downloading item X of Y"
                    if (line.contains("[download] Downloading item ")) {
                        try {
                            String progress = line.substring(line.indexOf("[download] Downloading item ") + 28);
                            String[] parts = progress.split(" of ");
                            if (parts.length == 2) {
                                job.setCurrentItem(Integer.parseInt(parts[0].trim()));
                                job.setTotalItems(Integer.parseInt(parts[1].trim()));
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    // Ghi nhận lỗi tải phụ đề (Vấn đề không nghiêm trọng, không dừng hệ thống)
                    if (line.contains("Unable to download video subtitles") && line.contains("429")) {
                        job.addLog("Subtitle error (rate limit) - continuing...");
                    }

                    // Bắt các lỗi nghiêm trọng (Ngoại trừ lỗi phụ đề)
                    if (line.startsWith("ERROR:") && !line.contains("subtitles")) {
                        job.setErrorMessage(line);
                    }
                }
            }

            int exitCode = process.waitFor();

            // Chỉ đánh dấu thất bại nếu exitCode lỗi KHÁC 0 VÀ đó không phải là do rớt tải phụ đề
            if (exitCode != 0) {
                // Kiểm tra lại log xem thực tế tải đã hoàn thành chưa dù có thông báo lỗi
                List<String> logs = job.getLogs();
                boolean downloadFinished = false;

                if (logs != null && logs.size() > 0) {
                    for (int i = Math.max(0, logs.size() - 10); i < logs.size(); i++) {
                        if (logs.get(i).contains("Finished downloading playlist") ||
                                logs.get(i).contains("[download] 100%")) {
                            downloadFinished = true;
                            break;
                        }
                    }
                }

                if (!downloadFinished) {
                    throw new RuntimeException("yt-dlp exited with code " + exitCode);
                }
            }

        } catch (Exception e) {
            job.setErrorMessage(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void cleanUpStaleFiles(Path dir) {
        try {
            Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        // Xoá các file tải dở .part hoặc .ytdl (các bản nháp tải chưa hoàn tất)
                        if (name.endsWith(".part") || name.endsWith(".ytdl")) {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                            }
                        }
                    });
        } catch (Exception e) {
            // Lỗi xóa dọn file rác không quá nghiêm trọng (Non-critical cleanup)
        }
    }
}
