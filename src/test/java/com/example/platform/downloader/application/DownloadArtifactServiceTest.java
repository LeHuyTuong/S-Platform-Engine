package com.example.platform.downloader.application;

import com.example.platform.downloader.application.job.JobEventService;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.infrastructure.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
class DownloadArtifactServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void cleanupFailedArtifactsDeletesEmptyJobDirectory() throws Exception {
        AppSettings appSettings = new AppSettings(tempDir.toString());
        Job job = new Job("https://example.com/video");
        job.setDownloadPath(Path.of("jobs", job.getId()).toString());
        JobRepository jobRepository = stubJobRepository(job);
        DownloadArtifactService service = new DownloadArtifactService(
                jobRepository,
                null,
                appSettings,
                null,
                tempDir.toString(),
                "ffmpeg"
        );

        Path jobDir = tempDir.resolve(job.getDownloadPath());
        Files.createDirectories(jobDir);
        Files.writeString(jobDir.resolve("video.part"), "partial");
        Files.writeString(jobDir.resolve("video.ytdl"), "state");

        service.cleanupFailedArtifacts(job);

        assertThat(jobDir).doesNotExist();
    }

    private JobRepository stubJobRepository(Job job) {
        return (JobRepository) Proxy.newProxyInstance(
                JobRepository.class.getClassLoader(),
                new Class<?>[]{JobRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName()) && args != null && args.length == 1 && job.getId().equals(args[0])) {
                        return Optional.of(job);
                    }
                    if ("findById".equals(method.getName())) {
                        return Optional.empty();
                    }
                    if ("toString".equals(method.getName())) {
                        return "StubJobRepository";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
