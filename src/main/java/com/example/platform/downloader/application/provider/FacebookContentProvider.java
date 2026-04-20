package com.example.platform.downloader.application.provider;

import com.example.platform.downloader.domain.Platform;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.infrastructure.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FacebookContentProvider extends AbstractYtDlpProvider {

    public FacebookContentProvider(ObjectMapper objectMapper,
                                   AppSettings appSettings,
                                   WorkerProperties workerProperties,
                                   @Value("${app.downloader.yt-dlp-path:yt-dlp}") String ytDlpPath,
                                   @Value("${app.downloader.ffmpeg-path:ffmpeg}") String ffmpegPath) {
        super(objectMapper, appSettings, workerProperties, ytDlpPath, ffmpegPath);
    }

    @Override
    public Platform platform() {
        return Platform.FACEBOOK;
    }

    @Override
    public boolean supports(String url) {
        String host = host(url);
        return host.contains("facebook.com") || host.contains("fb.watch");
    }
}
