package com.example.platform.downloader.application;

import com.example.platform.downloader.application.job.JobEventService;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.infrastructure.JobRepository;
import com.example.platform.downloader.infrastructure.StoredAssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadArtifactServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private StoredAssetRepository storedAssetRepository;

    @Mock
    private JobEventService jobEventService;

    @TempDir
    Path tempDir;

    @Test
    void cleanupFailedArtifactsDeletesEmptyJobDirectory() throws Exception {
        AppSettings appSettings = new AppSettings(tempDir.toString());
        DownloadArtifactService service = new DownloadArtifactService(
                jobRepository,
                storedAssetRepository,
                appSettings,
                jobEventService,
                tempDir.toString(),
                "ffmpeg"
        );

        Job job = new Job("https://example.com/video");
        job.setDownloadPath(Path.of("jobs", job.getId()).toString());

        Path jobDir = tempDir.resolve(job.getDownloadPath());
        Files.createDirectories(jobDir);
        Files.writeString(jobDir.resolve("video.part"), "partial");
        Files.writeString(jobDir.resolve("video.ytdl"), "state");

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        service.cleanupFailedArtifacts(job);

        assertThat(jobDir).doesNotExist();
    }
}
