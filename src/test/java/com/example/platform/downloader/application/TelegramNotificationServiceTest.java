package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.infrastructure.AppSettings;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TelegramNotificationServiceTest {

    @Test
    void resolveVideoPathUsesJobDownloadPathWhenPresent() {
        TelegramNotificationService service = new TelegramNotificationService(
                mock(UserConnectionSettingsService.class),
                mock(AppSettings.class),
                "downloads",
                "http://localhost:8080"
        );

        Job job = new Job("https://example.com/video");
        job.setDownloadPath(Path.of("jobs", "custom-folder").toString());

        Path resolved = service.resolveVideoPath(job, "video.mp4");

        assertThat(resolved).isEqualTo(Path.of("downloads", "jobs", "custom-folder", "video.mp4"));
    }

    @Test
    void resolveVideoPathFallsBackToJobIdFolder() {
        TelegramNotificationService service = new TelegramNotificationService(
                mock(UserConnectionSettingsService.class),
                mock(AppSettings.class),
                "downloads",
                "http://localhost:8080"
        );

        Job job = new Job("https://example.com/video");

        Path resolved = service.resolveVideoPath(job, "video.mp4");

        assertThat(resolved).isEqualTo(Path.of("downloads", "jobs", job.getId(), "video.mp4"));
    }
}
