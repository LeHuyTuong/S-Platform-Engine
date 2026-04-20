package com.example.platform.downloader.application;

import com.example.platform.downloader.application.job.JobEventService;
import com.example.platform.downloader.application.provider.ContentProvider;
import com.example.platform.downloader.application.provider.ProgressSnapshot;
import com.example.platform.downloader.application.provider.ProviderRegistry;
import com.example.platform.downloader.domain.enums.FailureCategory;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.exception.ClassifiedDownloadException;
import com.example.platform.modules.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class DownloaderService {

    private final ProviderRegistry providerRegistry;
    private final DownloadArtifactService downloadArtifactService;
    private final JobEventService jobEventService;
    private final ProviderCredentialService providerCredentialService;
    private final String downloadDir;

    public DownloaderService(ProviderRegistry providerRegistry,
                             DownloadArtifactService downloadArtifactService,
                             JobEventService jobEventService,
                             ProviderCredentialService providerCredentialService,
                             @Value("${app.downloader.output-dir:downloads}") String downloadDir) {
        this.providerRegistry = providerRegistry;
        this.downloadArtifactService = downloadArtifactService;
        this.jobEventService = jobEventService;
        this.providerCredentialService = providerCredentialService;
        this.downloadDir = downloadDir;
    }

    public boolean hasCookieFile(User user) {
        return hasProviderCookie(user, Platform.YOUTUBE);
    }

    public boolean hasProviderCookie(User user, Platform platform) {
        return providerCredentialService.hasCookie(user, platform);
    }

    public void saveCookieFile(MultipartFile file, User user) throws IOException {
        saveProviderCookie(file, user, Platform.YOUTUBE);
    }

    public void saveProviderCookie(MultipartFile file, User user, Platform platform) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        providerCredentialService.saveCookie(user, platform, file.getOriginalFilename(), content);
    }

    public void deleteCookieFile(User user) {
        deleteProviderCookie(user, Platform.YOUTUBE);
    }

    public void deleteProviderCookie(User user, Platform platform) {
        providerCredentialService.deleteCookie(user, platform);
    }

    public List<Map<String, String>> listJobFiles(String jobId) {
        return downloadArtifactService.listJobFiles(jobId);
    }

    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> serveFile(String jobId,
                                                                                                   String filename) throws IOException {
        return downloadArtifactService.serveFile(jobId, filename);
    }

    public void cleanupFailedArtifacts(Job job) {
        downloadArtifactService.cleanupFailedArtifacts(job);
    }

    public void executeDownload(Job job, Duration timeout) {
        ContentProvider provider = resolveProvider(job);
        try {
            Path jobDir = downloadArtifactService.prepareJobDirectory(job);
            String cookieFile = materializeCookieFile(job);

            List<String> command = provider.buildDownloadCommand(job, jobDir, cookieFile);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Map<String, String> env = processBuilder.environment();
            String path = env.getOrDefault("Path", env.getOrDefault("PATH", ""));
            String localBin = new File("bin").getAbsolutePath();
            String nodePath = "C:\\nvm4w\\nodejs";
            env.put("Path", localBin + File.pathSeparator + nodePath + File.pathSeparator + path);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            readProcessOutput(job, provider, process);

            boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                job.setErrorMessage("Download timeout after " + timeout.toMinutes() + " minutes");
                throw new ClassifiedDownloadException(FailureCategory.TIMEOUT, job.getErrorMessage());
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                FailureCategory category = provider.classifyFailure(job.getLogs(), exitCode);
                job.setFailureCategory(category);
                throw new ClassifiedDownloadException(category, "yt-dlp exited with code " + exitCode);
            }

            downloadArtifactService.finalizeSuccessfulDownload(job);
            job.setProgressPercent(100.0);
            job.setDownloadSpeed(null);
            job.setEta(null);
            persistRuntimeProgress(job);
        } catch (ClassifiedDownloadException e) {
            throw e;
        } catch (Exception e) {
            throw new ClassifiedDownloadException(FailureCategory.PROCESS_ERROR, e.getMessage());
        }
    }

    private void readProcessOutput(Job job, ContentProvider provider, Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                job.addLog(line);
                jobEventService.appendInfo(job, line);

                ProgressSnapshot snapshot = provider.parseProgress(line);
                if (snapshot.getPlaylistTitle() != null) {
                    job.setPlaylistTitle(snapshot.getPlaylistTitle());
                }
                if (snapshot.getDetectedTitle() != null && (job.getVideoTitle() == null || job.getVideoTitle().isBlank())) {
                    job.setVideoTitle(snapshot.getDetectedTitle());
                }
                if (snapshot.getProgressPercent() != null) {
                    job.setProgressPercent(snapshot.getProgressPercent());
                }
                if (snapshot.getDownloadSpeed() != null) {
                    job.setDownloadSpeed(snapshot.getDownloadSpeed());
                }
                if (snapshot.getEta() != null) {
                    job.setEta(snapshot.getEta());
                }
                if (snapshot.getCurrentItem() != null) {
                    job.setCurrentItem(snapshot.getCurrentItem());
                }
                if (snapshot.getTotalItems() != null) {
                    job.setTotalItems(snapshot.getTotalItems());
                }
                if (line.startsWith("ERROR:") && !line.toLowerCase().contains("subtitles")) {
                    job.setErrorMessage(line);
                    jobEventService.appendError(job, line);
                }
                persistRuntimeProgress(job);
            }
        }
    }

    private String materializeCookieFile(Job job) throws IOException {
        if (job.getUser() == null || job.getPlatform() == null) {
            return null;
        }
        String content = providerCredentialService.loadCookie(job.getUser(), job.getPlatform()).orElse(null);
        if (content == null || content.isBlank()) {
            return null;
        }
        Path dir = Paths.get(downloadDir, "credentials", String.valueOf(job.getUser().getId()),
                job.getPlatform().name().toLowerCase());
        Files.createDirectories(dir);
        Path cookieFile = dir.resolve("cookies.txt");
        Files.writeString(cookieFile, content, StandardCharsets.UTF_8);
        return cookieFile.toAbsolutePath().toString();
    }

    private ContentProvider resolveProvider(Job job) {
        if (job.getPlatform() != null) {
            return providerRegistry.byPlatform(job.getPlatform());
        }
        return providerRegistry.detect(job.getUrl());
    }

    private void persistRuntimeProgress(Job job) {
        if (job.getId() == null) {
            return;
        }
        downloadArtifactService.persistRuntimeProgress(job);
    }
}
