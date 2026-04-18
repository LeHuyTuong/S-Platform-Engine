package com.example.platform.downloader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DownloaderService {

    private final JobManager jobManager;
    private final JobRepository jobRepository;
    private final String downloadDir;
    private final String cookieFile;
    private final AppSettings appSettings;

    public DownloaderService(JobManager jobManager,
            JobRepository jobRepository,
            @Value("${app.downloader.output-dir:downloads}") String downloadDir,
            AppSettings appSettings) {
        this.jobManager = jobManager;
        this.jobRepository = jobRepository;
        this.downloadDir = downloadDir;
        this.cookieFile = downloadDir + "/cookies.txt";
        this.appSettings = appSettings;
    }

    public Job submitDownload(java.util.Map<String, String> payload, com.example.platform.modules.user.domain.User currentUser) {
        String url = payload.get("url");

        // Fetch title trước khi submit để hiển thị ngay lập tức
        String preTitle = fetchVideoTitle(url);

        return jobManager.submitJob(url, currentUser, job -> {
            job.setDownloadType(payload.getOrDefault("type", "VIDEO"));
            job.setQuality(payload.getOrDefault("quality", "best"));
            job.setFormat(payload.getOrDefault("format", "mp4"));
            job.setProxy(payload.get("proxy"));
            job.setStartTime(payload.get("startTime"));
            job.setEndTime(payload.get("endTime"));
            job.setCleanMetadata("true".equalsIgnoreCase(payload.get("cleanMetadata")));
            // SEO & Thumbnail features
            job.setWriteThumbnail("true".equalsIgnoreCase(payload.get("writeThumbnail")));
            job.setWatermarkText(payload.get("watermarkText"));
            job.setTitleTemplate(payload.get("titleTemplate"));
            // Set title sớm nhất có thể để hiển thị trong danh sách
            if (preTitle != null && !preTitle.isBlank()) {
                job.setVideoTitle(preTitle);
                jobRepository.save(job); // Lưu ngay vào DB
            }
            executeDownload(job);
        });
    }

    /**
     * Gọi YouTube oEmbed API để lấy tiêu đề video - không cần API key, rất nhanh.
     * Trả về null nếu URL không phải YouTube hoặc gặp lỗi.
     */
    public String fetchVideoTitle(String inputUrl) {
        try {
            String encodedUrl = URLEncoder.encode(inputUrl, StandardCharsets.UTF_8);
            String apiUrl = "https://www.youtube.com/oembed?url=" + encodedUrl + "&format=json";
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (conn.getResponseCode() == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                // Parse "title":"VALUE" from JSON (no extra dep needed)
                Matcher m = Pattern.compile("\"title\":\"([^\"]+)\"").matcher(sb);
                if (m.find()) return m.group(1)
                    .replace("\\u0026", "&")
                    .replace("\\u003c", "<")
                    .replace("\\u003e", ">");
            }
        } catch (Exception ignored) {}
        return null;
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
            
            // MMO Features: Strip all metadata for Re-up safety if requested
            if (job.isCleanMetadata()) {
                command.add("--postprocessor-args");
                command.add("ffmpeg:-map_metadata -1");
            }
            
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

            // Khuôn mẫu tên file xuất ra (hỗ trợ SEO Title Template và playlist)
            String outputTemplate;
            if (job.getTitleTemplate() != null && !job.getTitleTemplate().isEmpty()) {
                // Dùng mẫu tiêu đề tuỳ chỉnh: {title}, {channel}, {id} → %(title)s, %(channel)s, %(id)s
                String tpl = job.getTitleTemplate()
                    .replace("{title}", "%(title)s")
                    .replace("{channel}", "%(channel)s")
                    .replace("{date}", "%(upload_date)s")
                    .replace("{id}", "%(id)s")
                    .replace("{resolution}", "%(height)sp");
                outputTemplate = downloadDir + "/%(playlist)s/" + tpl + " [%(id)s].%(ext)s";
            } else {
                outputTemplate = downloadDir + "/%(playlist)s/%(playlist_index)03d - %(title).200B [%(id)s].%(ext)s";
            }
            command.add("-o");
            command.add(outputTemplate);

            // Thumbnail: luôn tải nếu writeThumbnail hoặc có watermark
            if (job.isWriteThumbnail() || (job.getWatermarkText() != null && !job.getWatermarkText().isEmpty())) {
                command.add("--write-thumbnail");
                command.add("--convert-thumbnails");
                command.add("jpg");
            }

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

                    // Trích xuất tiêu đề video từ dòng Destination (tên file thực tế)
                    // Ví dụ: [download] Destination: downloads/NA/001 - Ten Video [abc123].mp4
                    if (line.startsWith("[download] Destination:") && job.getVideoTitle() == null) {
                        try {
                            String dest = line.substring("[download] Destination:".length()).trim();
                            String filename = Paths.get(dest).getFileName().toString();
                            // Xoá phần extension: .mp4, .mkv, ...
                            int lastDot = filename.lastIndexOf('.');
                            if (lastDot > 0) filename = filename.substring(0, lastDot);
                            // Xoá phần [id] ở cuối: " [xxxxxxxx]"
                            filename = filename.replaceAll("\\s*\\[[A-Za-z0-9_\\-]+\\]\\s*$", "").trim();
                            // Xoá prefix số thứ tự playlist "001 - " 
                            filename = filename.replaceAll("^\\d{1,3}\\s*-\\s*", "").trim();
                            if (!filename.isEmpty()) {
                                job.setVideoTitle(filename);
                            }
                        } catch (Exception ignored) {}
                    }

                    // Phân tích tốc độ tải và ETA thời gian thực từ yt-dlp output
                    // Định dạng mẫu: [download]  45.3% of 123.45MiB at 2.50MiB/s ETA 00:23
                    if (line.startsWith("[download]") && line.contains("% of") && line.contains(" at ")) {
                        try {
                            // Trích xuất phần trăm
                            String pctStr = line.substring(line.indexOf(']') + 1, line.indexOf('%')).trim();
                            job.setProgressPercent(Double.parseDouble(pctStr));
                            
                            // Trích xuất tốc độ (at X.XXMiB/s)
                            int atIdx = line.indexOf(" at ");
                            if (atIdx != -1) {
                                String afterAt = line.substring(atIdx + 4).trim();
                                String speed = afterAt.split(" ")[0];
                                job.setDownloadSpeed(speed);
                            }
                            
                            // Trích xuất ETA
                            int etaIdx = line.indexOf("ETA ");
                            if (etaIdx != -1) {
                                String etaVal = line.substring(etaIdx + 4).trim().split(" ")[0];
                                job.setEta(etaVal);
                            }
                        } catch (Exception ignored) {}
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

        // Sau khi tải xong: áp dụng watermark lên tất cả thumbnail tìm thấy trong thư mục
        if (job.getWatermarkText() != null && !job.getWatermarkText().isEmpty()) {
            applyWatermarkToThumbnails(job);
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

    /**
     * Đóng dấu watermark (chữ) lên tất cả file .jpg thumbnail trong thư mục tải.
     * Sử dụng FFmpeg drawtext filter để an toàn và nhẹ.
     */
    private void applyWatermarkToThumbnails(Job job) {
        try {
            String watermark = job.getWatermarkText();
            if (watermark == null || watermark.isEmpty()) return;

            Files.walk(Paths.get(downloadDir))
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jpg"))
                .forEach(thumbnailPath -> {
                    try {
                        String inPath = thumbnailPath.toAbsolutePath().toString();
                        String outPath = inPath.replace(".jpg", "_wm.jpg");

                        // FFmpeg: vẽ chữ watermark góc dưới phải, nền đen mờ
                        List<String> cmd = new ArrayList<>();
                        cmd.add("ffmpeg");
                        cmd.add("-y");
                        cmd.add("-i"); cmd.add(inPath);
                        cmd.add("-vf");
                        cmd.add("drawtext=text='" + watermark.replace("'", "\\'") + "'" +
                                ":fontsize=36:fontcolor=white:x=w-tw-20:y=h-th-20" +
                                ":box=1:boxcolor=black@0.55:boxborderw=8");
                        cmd.add("-q:v"); cmd.add("2");
                        cmd.add(outPath);

                        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
                        p.waitFor();

                        // Thay thế file gốc bằng file đã watermark
                        if (new File(outPath).exists()) {
                            Files.deleteIfExists(thumbnailPath);
                            new File(outPath).renameTo(thumbnailPath.toFile());
                            job.addLog("[WATERMARK] Đã đóng dấu thumbnail: " + thumbnailPath.getFileName());
                        }
                    } catch (Exception e) {
                        job.addLog("[WATERMARK] Lỗi: " + e.getMessage());
                    }
                });
        } catch (Exception e) {
            job.addLog("[WATERMARK] Không thể quét thư mục thumbnail: " + e.getMessage());
        }
    }
}
