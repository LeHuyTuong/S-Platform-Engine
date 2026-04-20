package com.example.platform.downloader.application;

import com.example.platform.downloader.application.job.JobEventService;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.entity.StoredAsset;
import com.example.platform.downloader.domain.enums.FailureCategory;
import com.example.platform.downloader.domain.enums.StoredAssetType;
import com.example.platform.downloader.exception.ClassifiedDownloadException;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.StoredAssetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class DownloadArtifactService {

    private final JobRepository jobRepository;
    private final StoredAssetRepository storedAssetRepository;
    private final AppSettings appSettings;
    private final JobEventService jobEventService;
    private final String downloadDir;
    private final String ffmpegPath;
    private final ObjectMapper objectMapper;

    public DownloadArtifactService(JobRepository jobRepository,
                                   StoredAssetRepository storedAssetRepository,
                                   AppSettings appSettings,
                                   JobEventService jobEventService,
                                   @Value("${app.downloader.output-dir:downloads}") String downloadDir,
                                   @Value("${app.downloader.ffmpeg-path:ffmpeg}") String ffmpegPath) {
        this.jobRepository = jobRepository;
        this.storedAssetRepository = storedAssetRepository;
        this.appSettings = appSettings;
        this.jobEventService = jobEventService;
        this.downloadDir = downloadDir;
        this.ffmpegPath = resolveBinaryPath(ffmpegPath);
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<Map<String, String>> listJobFiles(String jobId) {
        Path jobDir = jobDirectory(jobId);
        if (!Files.exists(jobDir)) {
            return Collections.emptyList();
        }

        List<Map<String, String>> files = new ArrayList<>();
        try {
            Files.walk(jobDir)
                    .filter(Files::isRegularFile)
                    .filter(this::isDownloadArtifact)
                    .forEach(path -> {
                        String relative = jobDir.relativize(path).toString().replace("\\", "/");
                        try {
                            files.add(Map.of(
                                    "name", path.getFileName().toString(),
                                    "path", relative,
                                    "size", String.valueOf(Files.size(path)),
                                    "contentType", defaultString(Files.probeContentType(path), "application/octet-stream")
                            ));
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
        return files;
    }

    public ResponseEntity<Resource> serveFile(String jobId, String filename) throws IOException {
        String safeName = Paths.get(filename).getFileName().toString();
        Path jobDir = jobDirectory(jobId);
        if (!Files.exists(jobDir)) {
            return null;
        }

        Path found = Files.walk(jobDir)
                .filter(Files::isRegularFile)
                .filter(this::isDownloadArtifact)
                .filter(path -> path.getFileName().toString().equals(safeName))
                .findFirst()
                .orElse(null);
        if (found == null) {
            return null;
        }

        InputStream inputStream = Files.newInputStream(found);
        Resource resource = new InputStreamResource(inputStream);
        String contentType = defaultString(Files.probeContentType(found), "application/octet-stream");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(Files.size(found))
                .body(resource);
    }

    public Path prepareJobDirectory(Job job) throws IOException {
        Path jobDir = ensureJobDirectory(job);
        Files.createDirectories(jobDir);
        cleanUpStaleFiles(jobDir);
        return jobDir;
    }

    public void finalizeSuccessfulDownload(Job job) {
        if (job.getWatermarkText() != null && !job.getWatermarkText().isBlank()) {
            applyWatermarkToThumbnails(job);
        }
        syncStoredAssets(job);
        enforceMaxFileSize(job);
        generateManifest(job);
    }

    public void cleanupFailedArtifacts(Job job) {
        Path jobDir = jobDirectory(job.getId());
        if (!Files.exists(jobDir)) {
            return;
        }
        cleanUpStaleFiles(jobDir);
        deleteEmptyDirectories(jobDir);
        try {
            if (Files.exists(jobDir) && isDirectoryEmpty(jobDir)) {
                Files.deleteIfExists(jobDir);
            }
        } catch (IOException ignored) {
        }
    }

    public void persistRuntimeProgress(Job job) {
        jobRepository.updateRuntimeProgress(
                job.getId(),
                job.getProgressPercent(),
                job.getDownloadSpeed(),
                job.getEta(),
                job.getPlaylistTitle(),
                job.getVideoTitle(),
                job.getCurrentItem(),
                job.getTotalItems(),
                job.getErrorMessage()
        );
    }

    private void syncStoredAssets(Job job) {
        Path jobDir = jobDirectory(job.getId());
        storedAssetRepository.deleteByJobId(job.getId());
        try {
            if (!Files.exists(jobDir)) {
                return;
            }
            Files.walk(jobDir)
                    .filter(Files::isRegularFile)
                    .filter(this::isDownloadArtifact)
                    .forEach(path -> {
                        StoredAsset asset = new StoredAsset();
                        asset.setJob(job);
                        asset.setFileName(path.getFileName().toString());
                        asset.setRelativePath(jobDir.relativize(path).toString().replace("\\", "/"));
                        asset.setContentType(defaultString(safeProbeContentType(path), "application/octet-stream"));
                        asset.setSizeBytes(safeSize(path));
                        asset.setAssetType(resolveAssetType(path.getFileName().toString()));
                        storedAssetRepository.save(asset);
                    });
        } catch (IOException e) {
            jobEventService.appendWarn(job, "Khong the scan assets: " + e.getMessage());
        }
    }

    private void enforceMaxFileSize(Job job) {
        long maxMb = appSettings.getMaxFileSizeMb();
        if (maxMb <= 0) {
            return;
        }
        long totalBytes = storedAssetRepository.findByJobIdOrderByCreatedAtAsc(job.getId()).stream()
                .mapToLong(StoredAsset::getSizeBytes)
                .sum();
        long maxBytes = maxMb * 1024 * 1024;
        if (totalBytes > maxBytes) {
            throw new ClassifiedDownloadException(
                    FailureCategory.PROCESS_ERROR,
                    "Kich thuoc tai ve vuot gioi han " + maxMb + "MB"
            );
        }
    }

    private void generateManifest(Job job) {
        Path jobDir = jobDirectory(job.getId());
        if (!Files.exists(jobDir)) {
            return;
        }
        try {
            List<Map<String, Object>> files = new ArrayList<>();
            for (StoredAsset asset : storedAssetRepository.findByJobIdOrderByCreatedAtAsc(job.getId())) {
                files.add(Map.of(
                        "name", asset.getFileName(),
                        "path", asset.getRelativePath(),
                        "size", asset.getSizeBytes(),
                        "type", asset.getAssetType().name(),
                        "contentType", defaultString(asset.getContentType(), "application/octet-stream")
                ));
            }

            Map<String, Object> manifest = new HashMap<>();
            manifest.put("jobId", job.getId());
            manifest.put("platform", job.getPlatform() != null ? job.getPlatform().name() : null);
            manifest.put("sourceUrl", job.getUrl());
            manifest.put("externalItemId", job.getExternalItemId());
            manifest.put("title", job.getVideoTitle());
            manifest.put("author", job.getAuthorName());
            manifest.put("caption", job.getCaptionText());
            manifest.put("publishedAt", job.getPublishedAt());
            manifest.put("duration", job.getDurationSeconds());
            manifest.put("thumbnail", job.getThumbnailUrl());
            manifest.put("availability", job.getAvailability());
            manifest.put("files", files);

            objectMapper.writeValue(jobDir.resolve("manifest.json").toFile(), manifest);
        } catch (Exception e) {
            jobEventService.appendWarn(job, "Khong the tao manifest.json: " + e.getMessage());
        }
    }

    private Path ensureJobDirectory(Job job) {
        if (job.getDownloadPath() == null || job.getDownloadPath().isBlank()) {
            job.setDownloadPath(relativeJobDirectory(job.getId()));
            jobRepository.save(job);
        }
        return resolveJobDirectory(job.getDownloadPath());
    }

    private Path jobDirectory(String jobId) {
        return jobRepository.findById(jobId)
                .map(this::jobDirectory)
                .orElseGet(() -> resolveJobDirectory(relativeJobDirectory(jobId)));
    }

    private Path jobDirectory(Job job) {
        String relativePath = (job.getDownloadPath() == null || job.getDownloadPath().isBlank())
                ? relativeJobDirectory(job.getId())
                : job.getDownloadPath();
        return resolveJobDirectory(relativePath);
    }

    private Path resolveJobDirectory(String relativePath) {
        return Paths.get(downloadDir).resolve(relativePath).normalize();
    }

    private String relativeJobDirectory(String folderName) {
        return Paths.get("jobs", folderName).toString();
    }

    private boolean isDownloadArtifact(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return !name.endsWith(".part")
                && !name.endsWith(".ytdl")
                && !name.equals("downloaded.txt");
    }

    private void cleanUpStaleFiles(Path jobDir) {
        try {
            Files.walk(jobDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (name.endsWith(".part") || name.endsWith(".ytdl")) {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private void deleteEmptyDirectories(Path rootDir) {
        try {
            Files.walk(rootDir)
                    .sorted((left, right) -> Integer.compare(right.getNameCount(), left.getNameCount()))
                    .filter(Files::isDirectory)
                    .forEach(path -> {
                        try {
                            if (isDirectoryEmpty(path)) {
                                Files.deleteIfExists(path);
                            }
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private boolean isDirectoryEmpty(Path path) throws IOException {
        try (Stream<Path> children = Files.list(path)) {
            return children.findAny().isEmpty();
        }
    }

    private void applyWatermarkToThumbnails(Job job) {
        Path jobDir = jobDirectory(job.getId());
        if (!Files.exists(jobDir)) {
            return;
        }
        try {
            Files.walk(jobDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jpg"))
                    .forEach(thumbnailPath -> {
                        try {
                            String inPath = thumbnailPath.toAbsolutePath().toString();
                            String outPath = inPath.replace(".jpg", "_wm.jpg");
                            List<String> command = new ArrayList<>();
                            command.add(ffmpegPath);
                            command.add("-y");
                            command.add("-i");
                            command.add(inPath);
                            command.add("-vf");
                            command.add("drawtext=text='" + job.getWatermarkText().replace("'", "\\'") + "'"
                                    + ":fontsize=36:fontcolor=white:x=w-tw-20:y=h-th-20"
                                    + ":box=1:boxcolor=black@0.55:boxborderw=8");
                            command.add("-q:v");
                            command.add("2");
                            command.add(outPath);

                            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
                            process.waitFor();

                            if (Files.exists(Path.of(outPath))) {
                                Files.deleteIfExists(thumbnailPath);
                                Files.move(Path.of(outPath), thumbnailPath);
                                jobEventService.appendInfo(job, "[WATERMARK] Da dong dau thumbnail: " + thumbnailPath.getFileName());
                            }
                        } catch (Exception e) {
                            jobEventService.appendWarn(job, "[WATERMARK] Loi: " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            jobEventService.appendWarn(job, "[WATERMARK] Khong the quet thumbnail: " + e.getMessage());
        }
    }

    private String resolveBinaryPath(String path) {
        if (new File(path).exists()) {
            return new File(path).getAbsolutePath();
        }
        return path.replace(".exe", "");
    }

    private StoredAssetType resolveAssetType(String fileName) {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg") || normalized.endsWith(".png") || normalized.endsWith(".webp")) {
            return StoredAssetType.THUMBNAIL;
        }
        if (normalized.endsWith(".vtt") || normalized.endsWith(".srt")) {
            return StoredAssetType.SUBTITLE;
        }
        if (normalized.endsWith(".description")) {
            return StoredAssetType.DESCRIPTION;
        }
        if (normalized.endsWith(".json")) {
            return StoredAssetType.MANIFEST;
        }
        if (normalized.endsWith(".mp4") || normalized.endsWith(".mkv") || normalized.endsWith(".webm")
                || normalized.endsWith(".mov") || normalized.endsWith(".mp3") || normalized.endsWith(".m4a")) {
            return StoredAssetType.MEDIA;
        }
        return StoredAssetType.OTHER;
    }

    private String safeProbeContentType(Path path) {
        try {
            return Files.probeContentType(path);
        } catch (IOException e) {
            return null;
        }
    }

    private long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
