package com.example.platform.downloader.application.provider;

import com.example.platform.downloader.domain.enums.FailureCategory;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.domain.entity.SourceRequest;
import com.example.platform.downloader.domain.enums.SourceType;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.infrastructure.WorkerProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public abstract class AbstractYtDlpProvider implements ContentProvider {

    private static final int MAX_FAN_OUT = 200;

    protected final ObjectMapper objectMapper;
    protected final AppSettings appSettings;
    protected final WorkerProperties workerProperties;
    protected final String ytDlpPath;
    protected final String ffmpegPath;

    protected AbstractYtDlpProvider(ObjectMapper objectMapper,
                                    AppSettings appSettings,
                                    WorkerProperties workerProperties,
                                    @Value("${app.downloader.yt-dlp-path:yt-dlp}") String ytDlpPath,
                                    @Value("${app.downloader.ffmpeg-path:ffmpeg}") String ffmpegPath) {
        this.objectMapper = objectMapper;
        this.appSettings = appSettings;
        this.workerProperties = workerProperties;
        this.ytDlpPath = resolveBinaryPath(ytDlpPath);
        this.ffmpegPath = resolveBinaryPath(ffmpegPath);
    }

    @Override
    public SourceResolution resolveSource(SourceRequest request) {
        try {
            List<String> command = new ArrayList<>();
            command.add(ytDlpPath);
            command.add("--dump-single-json");
            command.add("--skip-download");
            command.add("--playlist-end");
            command.add(String.valueOf(MAX_FAN_OUT + 1));
            command.add("--no-warnings");
            command.add(request.getSourceUrl());

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0 || output.isEmpty()) {
                throw new IllegalStateException("yt-dlp resolve failed with code " + exitCode);
            }

            JsonNode root = objectMapper.readTree(output.toString());
            SourceResolution resolution = new SourceResolution();
            JsonNode entries = root.get("entries");
            if (entries != null && entries.isArray() && entries.size() > 0) {
                if (entries.size() > MAX_FAN_OUT) {
                    resolution.setBlocked(true);
                    resolution.setBlockedReason("Vượt giới hạn " + MAX_FAN_OUT + " mục trong một request.");
                    return resolution;
                }
                resolution.setResolvedSourceType(resolveFanoutType(request, root));
                for (JsonNode entry : entries) {
                    ResolvedItem item = toResolvedItem(entry, root);
                    if (item.getSourceUrl() != null && !item.getSourceUrl().isBlank()) {
                        resolution.getItems().add(item);
                    }
                }
            } else {
                resolution.setResolvedSourceType(SourceType.DIRECT_URL);
                resolution.getItems().add(toResolvedItem(root, root));
            }
            return resolution;
        } catch (Exception e) {
            throw new IllegalStateException("Không thể resolve source: " + e.getMessage(), e);
        }
    }

    protected SourceType resolveFanoutType(SourceRequest request, JsonNode root) {
        if (request.getSourceType() == SourceType.PLAYLIST || request.getSourceType() == SourceType.PROFILE) {
            return request.getSourceType();
        }
        String extractor = root.path("extractor_key").asText("").toLowerCase(Locale.ROOT);
        if (extractor.contains("playlist") || root.path("playlist_count").isNumber()) {
            return SourceType.PLAYLIST;
        }
        return SourceType.PROFILE;
    }

    protected ResolvedItem toResolvedItem(JsonNode node, JsonNode root) {
        ResolvedItem item = new ResolvedItem();
        item.setSourceUrl(resolveItemUrl(node).orElse(null));
        item.setExternalItemId(text(node, "id"));
        item.setTitle(text(node, "title"));
        item.setAuthor(firstNonBlank(text(node, "uploader"), text(node, "channel"), text(node, "creator")));
        item.setCaption(text(node, "description"));
        if (node.hasNonNull("timestamp")) {
            item.setPublishedAt(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(node.get("timestamp").asLong()),
                    ZoneId.systemDefault()
            ));
        }
        if (node.hasNonNull("duration")) {
            item.setDurationSeconds(node.get("duration").asLong());
        }
        item.setThumbnailUrl(firstNonBlank(text(node, "thumbnail"), text(root, "thumbnail")));
        item.setAvailability(firstNonBlank(text(node, "availability"), text(root, "availability"), "public"));
        item.setPlaylistTitle(firstNonBlank(text(root, "title"), text(root, "playlist_title")));
        return item;
    }

    protected Optional<String> resolveItemUrl(JsonNode node) {
        String webpageUrl = text(node, "webpage_url");
        if (webpageUrl != null && !webpageUrl.isBlank()) {
            return Optional.of(webpageUrl);
        }
        String originalUrl = text(node, "original_url");
        if (originalUrl != null && !originalUrl.isBlank()) {
            return Optional.of(originalUrl);
        }
        String directUrl = text(node, "url");
        if (directUrl != null && directUrl.startsWith("http")) {
            return Optional.of(directUrl);
        }
        String id = text(node, "id");
        return buildUrlFromId(id);
    }

    protected Optional<String> buildUrlFromId(String id) {
        return Optional.empty();
    }

    @Override
    public List<String> buildDownloadCommand(Job job, Path jobDir, String cookieFilePath) {
        List<String> command = new ArrayList<>();
        command.add(ytDlpPath);

        if ("AUDIO".equalsIgnoreCase(job.getDownloadType())) {
            command.add("-f");
            command.add("bestaudio");
            command.add("--extract-audio");
            command.add("--audio-format");
            command.add(job.getFormat() != null ? job.getFormat() : "mp3");
            command.add("--audio-quality");
            command.add("0");
        } else {
            command.add("-f");
            String resolution = job.getQuality();
            if (resolution == null || "best".equalsIgnoreCase(resolution)) {
                command.add("bv*+ba/b");
            } else {
                command.add("bv*[height<=" + resolution + "]+ba/b");
            }
            if (job.getFormat() != null && !job.getFormat().isBlank()) {
                command.add("--merge-output-format");
                command.add(job.getFormat());
            }
        }

        command.add("--embed-metadata");
        command.add("--write-description");
        if (job.isWriteThumbnail() || (job.getWatermarkText() != null && !job.getWatermarkText().isBlank())) {
            command.add("--write-thumbnail");
            command.add("--convert-thumbnails");
            command.add("jpg");
        }

        command.add("--embed-subs");
        command.add("--sub-langs");
        command.add("vi,en.*");

        if (job.isCleanMetadata()) {
            command.add("--postprocessor-args");
            command.add("FFmpeg:-map_metadata -1");
        }

        if (job.getStartTime() != null && !job.getStartTime().isBlank()
                && job.getEndTime() != null && !job.getEndTime().isBlank()) {
            command.add("--download-sections");
            command.add("*" + job.getStartTime() + "-" + job.getEndTime());
        }

        if (job.getProxy() != null && !job.getProxy().isBlank()) {
            command.add("--proxy");
            command.add(job.getProxy());
        }

        String outputTemplate;
        if (job.getTitleTemplate() != null && !job.getTitleTemplate().isBlank()) {
            String tpl = job.getTitleTemplate()
                    .replace("{title}", "%(title)s")
                    .replace("{channel}", "%(channel)s")
                    .replace("{date}", "%(upload_date)s")
                    .replace("{id}", "%(id)s")
                    .replace("{resolution}", "%(height)sp");
            outputTemplate = jobDir + "/%(playlist)s/" + tpl + " [%(id)s].%(ext)s";
        } else {
            outputTemplate = jobDir + "/%(playlist)s/%(playlist_index)03d - %(title).200B [%(id)s].%(ext)s";
        }

        command.add("-o");
        command.add(outputTemplate);
        command.add("--continue");
        command.add("--no-overwrites");
        command.add("--newline");
        command.add("--force-ipv4");
        command.add("--sleep-interval");
        command.add(String.valueOf(appSettings.getSleepInterval()));
        command.add("--sleep-requests");
        command.add(String.valueOf(appSettings.getSleepRequests()));
        command.add("--retries");
        command.add(String.valueOf(appSettings.getRetries()));
        command.add("--retry-sleep");
        command.add("5");
        command.add("--concurrent-fragments");
        command.add(String.valueOf(appSettings.getConcurrentFragments()));
        command.add("--verbose");

        if (cookieFilePath != null && !cookieFilePath.isBlank()) {
            command.add("--cookies");
            command.add(cookieFilePath);
        }

        command.add(job.getUrl());
        return command;
    }

    @Override
    public ProgressSnapshot parseProgress(String line) {
        ProgressSnapshot snapshot = new ProgressSnapshot();
        try {
            if (line.contains("Finished downloading playlist: ")) {
                snapshot.setPlaylistTitle(line.substring(line.indexOf("Finished downloading playlist: ") + 32));
            }

            if (line.startsWith("[download] Destination:")) {
                String dest = line.substring("[download] Destination:".length()).trim();
                String filename = Path.of(dest).getFileName().toString();
                int lastDot = filename.lastIndexOf('.');
                if (lastDot > 0) {
                    filename = filename.substring(0, lastDot);
                }
                filename = filename.replaceAll("\\s*\\[[A-Za-z0-9_\\-]+\\]\\s*$", "").trim();
                filename = filename.replaceAll("^\\d{1,3}\\s*-\\s*", "").trim();
                snapshot.setDetectedTitle(filename);
            }

            if (line.startsWith("[download]") && line.contains("% of") && line.contains(" at ")) {
                String pctStr = line.substring(line.indexOf(']') + 1, line.indexOf('%')).trim();
                snapshot.setProgressPercent(Double.parseDouble(pctStr));

                int atIdx = line.indexOf(" at ");
                if (atIdx != -1) {
                    String afterAt = line.substring(atIdx + 4).trim();
                    snapshot.setDownloadSpeed(afterAt.split(" ")[0]);
                }

                int etaIdx = line.indexOf("ETA ");
                if (etaIdx != -1) {
                    String etaVal = line.substring(etaIdx + 4).trim().split(" ")[0];
                    snapshot.setEta(etaVal);
                }
            }

            if (line.contains("[download] Downloading item ")) {
                String progress = line.substring(line.indexOf("[download] Downloading item ") + 28);
                String[] parts = progress.split(" of ");
                if (parts.length == 2) {
                    snapshot.setCurrentItem(Integer.parseInt(parts[0].trim()));
                    snapshot.setTotalItems(Integer.parseInt(parts[1].trim()));
                }
            }
        } catch (Exception ignored) {
        }
        return snapshot;
    }

    @Override
    public FailureCategory classifyFailure(List<String> logs, int exitCode) {
        String allLogs = String.join("\n", logs).toLowerCase(Locale.ROOT);
        if (allLogs.contains("429") || allLogs.contains("too many requests")) {
            return FailureCategory.RATE_LIMIT;
        }
        if (allLogs.contains("sign in to confirm you") || allLogs.contains("not a bot")) {
            return FailureCategory.RATE_LIMIT;
        }
        if (allLogs.contains("your ip address is blocked")) {
            return FailureCategory.PERMISSION_DENIED;
        }
        if (allLogs.contains("instagram sent an empty media response")
                || allLogs.contains("instagram api is not granting access")
                || allLogs.contains("use --cookies")
                || allLogs.contains("login required to access")) {
            return FailureCategory.PRIVATE_CONTENT;
        }
        if (allLogs.contains("cannot parse data")) {
            return FailureCategory.UNSUPPORTED;
        }
        if (allLogs.contains("unsupported url")) {
            return FailureCategory.UNSUPPORTED;
        }
        if (allLogs.contains("private video") || allLogs.contains("login required")) {
            return FailureCategory.PRIVATE_CONTENT;
        }
        if (allLogs.contains("requested format is not available") || allLogs.contains("unsupported")) {
            return FailureCategory.UNSUPPORTED;
        }
        if (allLogs.contains("404")
                || allLogs.contains("not available")
                || allLogs.contains("has been removed")
                || allLogs.contains("video unavailable")) {
            return FailureCategory.REMOVED;
        }
        if (allLogs.contains("403") || allLogs.contains("permission")) {
            return FailureCategory.PERMISSION_DENIED;
        }
        if (allLogs.contains("timed out")) {
            return FailureCategory.TIMEOUT;
        }
        if (exitCode != 0) {
            return FailureCategory.PROCESS_ERROR;
        }
        return FailureCategory.UNKNOWN;
    }

    protected String text(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    protected String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    protected String resolveBinaryPath(String path) {
        if (new File(path).exists()) {
            return new File(path).getAbsolutePath();
        }
        return path.replace(".exe", "");
    }

    protected String host(String url) {
        try {
            return URI.create(url).getHost().toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }
}
