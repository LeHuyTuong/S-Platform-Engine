package com.example.platform.downloader.application.provider;

import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.infrastructure.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class YouTubeContentProvider extends AbstractYtDlpProvider {

    public YouTubeContentProvider(ObjectMapper objectMapper,
                                  AppSettings appSettings,
                                  WorkerProperties workerProperties,
                                  @Value("${app.downloader.yt-dlp-path:yt-dlp}") String ytDlpPath,
                                  @Value("${app.downloader.ffmpeg-path:ffmpeg}") String ffmpegPath) {
        super(objectMapper, appSettings, workerProperties, ytDlpPath, ffmpegPath);
    }

    @Override
    public Platform platform() {
        return Platform.YOUTUBE;
    }

    @Override
    public boolean supports(String url) {
        String host = host(url);
        return host.contains("youtube.com") || host.contains("youtu.be");
    }

    @Override
    protected Optional<String> buildUrlFromId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.of("https://www.youtube.com/watch?v=" + id);
    }
}
