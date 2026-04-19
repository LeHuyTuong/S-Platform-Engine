package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.Job;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.AppSettings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.platform.modules.user.domain.User;

import jakarta.servlet.http.HttpSession;

@Service
public class DownloaderService {

    private final JobManager jobManager;
    private final JobRepository jobRepository;
    private final String downloadDir;
    private final AppSettings appSettings;
    private final String ytDlpPath;
    private final String ffmpegPath;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public DownloaderService(JobManager jobManager,
            JobRepository jobRepository,
            @Value("${app.downloader.output-dir:downloads}") String downloadDir,
            @Value("${app.downloader.yt-dlp-path:yt-dlp}") String ytDlpPath,
            @Value("${app.downloader.ffmpeg-path:ffmpeg}") String ffmpegPath,
            AppSettings appSettings) {
        this.jobManager = jobManager;
        this.jobRepository = jobRepository;
        this.downloadDir = downloadDir;
        this.ytDlpPath = resolveBinaryPath(ytDlpPath);
        this.ffmpegPath = resolveBinaryPath(ffmpegPath);
        this.appSettings = appSettings;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    }

    private String resolveBinaryPath(String path) {
        if (new File(path).exists()) {
            return new File(path).getAbsolutePath();
        }
        return path.replace(".exe", ""); // Fallback to global command (strip .exe for Linux compatibility if needed, though app.yml had .exe)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cookie management — per-user isolation (Feature 2 + Issue 2)
    // ─────────────────────────────────────────────────────────────────────────

    /** Đường dẫn cookie file riêng của từng user */
    private String cookieFilePath(User user) {
        return downloadDir + "/" + user.getId() + "/cookies.txt";
    }

    public boolean hasCookieFile(User user) {
        return new File(cookieFilePath(user)).exists();
    }

    public void saveCookieFile(MultipartFile file, User user) throws Exception {
        Path userDir = Paths.get(downloadDir, user.getId().toString());
        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
        }
        file.transferTo(new File(cookieFilePath(user)));
    }

    public void deleteCookieFile(User user) {
        new File(cookieFilePath(user)).delete();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Submit download
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Submit một download job.
     *
     * Bug 2 Fix: tất cả field (type/quality/format/title) được set trong
     * setupJob lambda chạy SYNC trên HTTP thread TRƯỚC khi JobManager gọi
     * jobRepository.save(). Đảm bảo DB record luôn có đầy đủ thông tin.
     *
     * Bug 5 Fix: fetchVideoTitle() giờ chạy bên trong executeDownload (executor
     * thread), không còn block HTTP thread ~4 giây nữa.
     */
    public Job submitDownload(Map<String, String> payload, User currentUser) {
        return submitDownload(payload, currentUser, null);
    }

    /**
     * Submit một download job (với HttpSession để truyền RuntimeSettings cho Telegram).
     */
    public Job submitDownload(Map<String, String> payload, User currentUser, HttpSession session) {
        String url = payload.get("url");

        // Dùng array trick để capture job reference cho executeJob lambda
        Job[] jobRef = new Job[1];

        return jobManager.submitJob(url, currentUser,
                // === setupJob: chạy SYNC trên HTTP thread — set field trước khi DB save ===
                job -> {
                    job.setDownloadType(payload.getOrDefault("type", "VIDEO"));
                    job.setQuality(payload.getOrDefault("quality", "best"));
                    job.setFormat(payload.getOrDefault("format", "mp4"));
                    job.setProxy(payload.get("proxy"));
                    job.setStartTime(payload.get("startTime"));
                    job.setEndTime(payload.get("endTime"));
                    job.setCleanMetadata("true".equalsIgnoreCase(payload.get("cleanMetadata")));
                    job.setWriteThumbnail("true".equalsIgnoreCase(payload.get("writeThumbnail")));
                    job.setWatermarkText(payload.get("watermarkText"));
                    job.setTitleTemplate(payload.get("titleTemplate"));
                    jobRef[0] = job; // capture reference cho executeJob
                },
                // === executeJob: chạy ASYNC trong executor thread ===
                () -> executeDownload(jobRef[0]),
                session
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File listing & serving (Feature 1)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Liệt kê các file đã tải về trong folder của job.
     * Chỉ trả về file thực (video/audio/thumbnail), bỏ qua tmp files.
     */
    public List<Map<String, String>> listJobFiles(String jobId) {
        Path jobDir = Paths.get(downloadDir, jobId);
        if (!Files.exists(jobDir)) {
            return Collections.emptyList();
        }
        List<Map<String, String>> files = new ArrayList<>();
        try {
            Files.walk(jobDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        // Bỏ qua file tạm và archive
                        return !name.endsWith(".part") && !name.endsWith(".ytdl")
                                && !name.equals("downloaded.txt");
                    })
                    .forEach(p -> {
                        String relative = jobDir.relativize(p).toString().replace("\\", "/");
                        long sizeBytes;
                        try { sizeBytes = Files.size(p); } catch (IOException e) { sizeBytes = 0; }
                        files.add(Map.of(
                                "name", p.getFileName().toString(),
                                "path", relative,
                                "size", String.valueOf(sizeBytes)
                        ));
                    });
        } catch (IOException e) {
            // Thư mục không tồn tại hoặc lỗi đọc — trả empty list
        }
        return files;
    }

    /**
     * Stream file về client dưới dạng download attachment.
     * Trả null nếu file không tồn tại (controller sẽ trả 404).
     */
    public ResponseEntity<Resource> serveFile(String jobId, String filename) throws IOException {
        // Sanitize filename để tránh path traversal
        String safeName = Paths.get(filename).getFileName().toString();
        // Tìm file theo tên trong toàn bộ subfolder của jobId
        Path jobDir = Paths.get(downloadDir, jobId);
        if (!Files.exists(jobDir)) return null;

        Path[] found = {null};
        try {
            Files.walk(jobDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(safeName))
                    .findFirst()
                    .ifPresent(p -> found[0] = p);
        } catch (IOException e) {
            return null;
        }

        if (found[0] == null) return null;

        Path filePath = found[0];
        InputStream is = Files.newInputStream(filePath);
        InputStreamResource resource = new InputStreamResource(is);

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "application/octet-stream";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(Files.size(filePath))
                .body(resource);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Title fetching
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Gọi YouTube oEmbed API để lấy tiêu đề video.
     * Không cần API key. Trả về null nếu lỗi hoặc không phải YouTube.
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
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                Matcher m = Pattern.compile("\"title\":\"([^\"]+)\"").matcher(sb);
                if (m.find()) return m.group(1)
                        .replace("\\u0026", "&")
                        .replace("\\u003c", "<")
                        .replace("\\u003e", ">");
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core download logic
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Chạy yt-dlp để tải video/audio cho job.
     * Mỗi job tải vào subfolder riêng downloads/{jobId}/ (fix Issue 1: stale .part)
     * Archive file per-user để tránh skip chéo (fix Issue 2).
     * Bug 5 Fix: fetchVideoTitle chạy ở đây (async thread), không block HTTP thread.
     */
    public void executeDownload(Job job) {
        try {
            // Issue 1 Fix: mỗi job có subfolder riêng để tránh .part conflict
            Path jobOutputDir = Paths.get(downloadDir, job.getId());
            if (!Files.exists(jobOutputDir)) {
                Files.createDirectories(jobOutputDir);
            }

            // Xoá .part cũ CHỈ trong subfolder của chính job này
            cleanUpStaleFiles(jobOutputDir);

            // Bug 5 Fix: fetch title async tại đây (đang ở executor thread)
            if (job.getVideoTitle() == null) {
                String title = fetchVideoTitle(job.getUrl());
                if (title != null && !title.isBlank()) {
                    job.setVideoTitle(title);
                    jobRepository.save(job);
                }
            }

            // Issue 2 Fix: archive file riêng theo userId để tránh skip chéo giữa users
            String userId = job.getUser() != null ? job.getUser().getId().toString() : "shared";
            Path userDir = Paths.get(downloadDir, userId);
            if (!Files.exists(userDir)) Files.createDirectories(userDir);
            String archiveFile = userDir.resolve("downloaded.txt").toString();

            List<String> command = new ArrayList<>();
            command.add(ytDlpPath);

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
                command.add(ffmpegPath + ":-map_metadata -1");
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

            // Issue 1 Fix: output vào subfolder riêng của job
            String jobDirStr = jobOutputDir.toString();
            String outputTemplate;
            if (job.getTitleTemplate() != null && !job.getTitleTemplate().isEmpty()) {
                String tpl = job.getTitleTemplate()
                    .replace("{title}", "%(title)s")
                    .replace("{channel}", "%(channel)s")
                    .replace("{date}", "%(upload_date)s")
                    .replace("{id}", "%(id)s")
                    .replace("{resolution}", "%(height)sp");
                outputTemplate = jobDirStr + "/%(playlist)s/" + tpl + " [%(id)s].%(ext)s";
            } else {
                outputTemplate = jobDirStr + "/%(playlist)s/%(playlist_index)03d - %(title).200B [%(id)s].%(ext)s";
            }
            command.add("-o");
            command.add(outputTemplate);

            // Thumbnail: luôn tải nếu writeThumbnail hoặc có watermark
            if (job.isWriteThumbnail() || (job.getWatermarkText() != null && !job.getWatermarkText().isEmpty())) {
                command.add("--write-thumbnail");
                command.add("--convert-thumbnails");
                command.add("jpg");
            }

            // Cấu hình hiệu năng & chống chặn - đọc từ AppSettings
            command.add("--concurrent-fragments");
            command.add(String.valueOf(appSettings.getConcurrentFragments()));
            command.add("--sleep-interval");
            command.add(String.valueOf(appSettings.getSleepInterval()));
            command.add("--retries");
            command.add(String.valueOf(appSettings.getRetries()));
            command.add("--retry-sleep");
            command.add("5");

            // Cookie file per-user (Feature 2 Fix)
            if (job.getUser() != null && hasCookieFile(job.getUser())) {
                String cf = cookieFilePath(job.getUser());
                command.add("--cookies");
                command.add(cf);
                job.addLog("Using cookies file: " + cf);
            }

            // In mỗi tiến trình trên một dòng mới để parse log dễ dàng
            command.add("--newline");

            // Chống ban: Ép dùng IPv4
            command.add("--force-ipv4");

            // Chống ban: Giả lập trình duyệt Chrome thật
            command.add("--user-agent");
            command.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            // Chống ban: Thời gian chờ giữa các lệnh API ngầm
            command.add("--sleep-requests");
            command.add(String.valueOf(appSettings.getSleepRequests()));

            // Gỡ lỗi
            command.add("--verbose");

            // URL
            command.add(job.getUrl());

            ProcessBuilder pb = new ProcessBuilder(command);
            
            // Fix: Add bin folder and Node.js folder to PATH so yt-dlp can find ffmpeg and JS runtime.
            // This resides in D:\Dev-Project\system-design\bin and the system Node path.
            Map<String, String> env = pb.environment();
            String path = env.getOrDefault("Path", "");
            String localBin = new File("bin").getAbsolutePath();
            String nodePath = "C:\\nvm4w\\nodejs";
            env.put("Path", localBin + File.pathSeparator + nodePath + File.pathSeparator + path);
            
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    job.addLog(line);

                    // Phân tích tên playlist
                    if (line.contains("Finished downloading playlist: ")) {
                        String title = line.substring(line.indexOf("Finished downloading playlist: ") + 32);
                        job.setPlaylistTitle(title);
                    }

                    // Trích xuất tiêu đề video từ dòng Destination (nếu chưa có từ oEmbed)
                    if (line.startsWith("[download] Destination:") && job.getVideoTitle() == null) {
                        try {
                            String dest = line.substring("[download] Destination:".length()).trim();
                            String filename = Paths.get(dest).getFileName().toString();
                            int lastDot = filename.lastIndexOf('.');
                            if (lastDot > 0) filename = filename.substring(0, lastDot);
                            filename = filename.replaceAll("\\s*\\[[A-Za-z0-9_\\-]+\\]\\s*$", "").trim();
                            filename = filename.replaceAll("^\\d{1,3}\\s*-\\s*", "").trim();
                            if (!filename.isEmpty()) {
                                job.setVideoTitle(filename);
                            }
                        } catch (Exception ignored) {}
                    }

                    // Phân tích tốc độ tải và ETA thời gian thực
                    if (line.startsWith("[download]") && line.contains("% of") && line.contains(" at ")) {
                        try {
                            String pctStr = line.substring(line.indexOf(']') + 1, line.indexOf('%')).trim();
                            job.setProgressPercent(Double.parseDouble(pctStr));

                            int atIdx = line.indexOf(" at ");
                            if (atIdx != -1) {
                                String afterAt = line.substring(atIdx + 4).trim();
                                job.setDownloadSpeed(afterAt.split(" ")[0]);
                            }

                            int etaIdx = line.indexOf("ETA ");
                            if (etaIdx != -1) {
                                String etaVal = line.substring(etaIdx + 4).trim().split(" ")[0];
                                job.setEta(etaVal);
                            }
                        } catch (Exception ignored) {}
                    }

                    // Phân tích quá trình tiến độ playlist
                    if (line.contains("[download] Downloading item ")) {
                        try {
                            String progress = line.substring(line.indexOf("[download] Downloading item ") + 28);
                            String[] parts = progress.split(" of ");
                            if (parts.length == 2) {
                                job.setCurrentItem(Integer.parseInt(parts[0].trim()));
                                job.setTotalItems(Integer.parseInt(parts[1].trim()));
                            }
                        } catch (Exception ignored) {}
                    }

                    // Ghi nhận lỗi tải phụ đề (không nghiêm trọng)
                    if (line.contains("Unable to download video subtitles") && line.contains("429")) {
                        job.addLog("Subtitle error (rate limit) - continuing...");
                    }

                    // Bắt các lỗi nghiêm trọng (ngoại trừ lỗi phụ đề)
                    if (line.startsWith("ERROR:") && !line.contains("subtitles")) {
                        job.setErrorMessage(line);
                    }
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                List<String> logs = job.getLogs();
                // Issue 3 Fix: scan toàn bộ log thay vì chỉ 10 dòng cuối
                boolean downloadFinished = logs != null && logs.stream().anyMatch(l ->
                        l.contains("Finished downloading playlist") || l.contains("[download] 100%"));

                if (!downloadFinished) {
                    throw new RuntimeException("yt-dlp exited with code " + exitCode);
                }
            }

        } catch (Exception e) {
            job.setErrorMessage(e.getMessage());
            throw new RuntimeException(e);
        }

        // Sau khi tải xong: apply watermark lên thumbnail
        if (job.getWatermarkText() != null && !job.getWatermarkText().isEmpty()) {
            applyWatermarkToThumbnails(job);
        }

        // Feature: Tạo meta.json cho Chỉnh sửa ảnh (Thumbnail SEO)
        generateMetaJson(job);
    }

    /**
     * Tạo file meta.json trong thư mục job chứa thông tin về các file ảnh đã tải.
     * Cần thiết để hỗ trợ "Chỉnh sửa ảnh" (Thumbnail Editor) tìm thấy file PNG/JPG.
     */
    private void generateMetaJson(Job job) {
        try {
            Path jobDir = Paths.get(downloadDir, job.getId());
            if (!Files.exists(jobDir)) return;

            List<String> imageFiles = new ArrayList<>();
            Files.walk(jobDir)
                .filter(Files::isRegularFile)
                .forEach(p -> {
                    String name = p.getFileName().toString();
                    String ext = name.toLowerCase();
                    if (ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".png") || ext.endsWith(".webp")) {
                        imageFiles.add(name);
                    }
                });

            Map<String, Object> meta = new HashMap<>();
            meta.put("jobId", job.getId());
            meta.put("url", job.getUrl());
            meta.put("title", job.getVideoTitle());
            meta.put("images", imageFiles);

            // Register each image as a valid entry to prevent "Not found in meta.json" errors
            for (String img : imageFiles) {
                meta.put(img, Map.of("processed", false, "timestamp", System.currentTimeMillis()));
            }

            Path metaPath = jobDir.resolve("meta.json");
            objectMapper.writeValue(metaPath.toFile(), meta);
            job.addLog("✅ Đã tạo meta.json cho thumbnail (" + imageFiles.size() + " ảnh)");

        } catch (Exception e) {
            job.addLog("⚠️ Cảnh báo: Không thể tạo meta.json: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Xoá các file tạm (.part, .ytdl) CHỈ trong thư mục của job hiện tại.
     * Issue 1 Fix: không quét toàn bộ downloads/ nữa để tránh xóa file của job khác.
     */
    private void cleanUpStaleFiles(Path jobDir) {
        try {
            Files.walk(jobDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        if (name.endsWith(".part") || name.endsWith(".ytdl")) {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {}
                        }
                    });
        } catch (Exception e) {
            // Non-critical cleanup
        }
    }

    /**
     * Đóng dấu watermark lên tất cả .jpg thumbnail trong folder của job.
     */
    private void applyWatermarkToThumbnails(Job job) {
        try {
            String watermark = job.getWatermarkText();
            if (watermark == null || watermark.isEmpty()) return;

            // Chỉ quét trong subfolder của job (không quét toàn bộ downloads/)
            Path jobDir = Paths.get(downloadDir, job.getId());
            if (!Files.exists(jobDir)) return;

            Files.walk(jobDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jpg"))
                .forEach(thumbnailPath -> {
                    try {
                        String inPath = thumbnailPath.toAbsolutePath().toString();
                        String outPath = inPath.replace(".jpg", "_wm.jpg");

                        List<String> cmd = new ArrayList<>();
                        cmd.add(ffmpegPath);
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
