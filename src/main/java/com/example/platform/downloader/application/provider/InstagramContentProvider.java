package com.example.platform.downloader.application.provider;

import com.example.platform.downloader.domain.Platform;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.infrastructure.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstagramContentProvider extends AbstractYtDlpProvider {

    public InstagramContentProvider(ObjectMapper objectMapper,
                                    AppSettings appSettings,
                                    WorkerProperties workerProperties,
                                    @Value("${app.downloader.yt-dlp-path:yt-dlp}") String ytDlpPath,
                                    @Value("${app.downloader.ffmpeg-path:ffmpeg}") String ffmpegPath) {
        super(objectMapper, appSettings, workerProperties, ytDlpPath, ffmpegPath);
    }

    @Override
    public Platform platform() {
        return Platform.INSTAGRAM;
    }

    @Override
    public boolean supports(String url) {
        return host(url).contains("instagram.com");
    }
}
