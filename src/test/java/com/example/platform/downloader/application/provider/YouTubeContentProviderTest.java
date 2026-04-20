package com.example.platform.downloader.application.provider;

import com.example.platform.downloader.domain.FailureCategory;
import com.example.platform.downloader.infrastructure.AppSettings;
import com.example.platform.downloader.infrastructure.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YouTubeContentProviderTest {

    private final YouTubeContentProvider provider = new YouTubeContentProvider(
            new ObjectMapper(),
            new AppSettings("build/test-downloads"),
            new WorkerProperties(),
            "yt-dlp",
            "ffmpeg"
    );

    @Test
    void classifyVideoUnavailableAsRemoved() {
        FailureCategory category = provider.classifyFailure(
                List.of("ERROR: [youtube] abc123: Video unavailable"),
                1
        );

        assertEquals(FailureCategory.REMOVED, category);
    }

    @Test
    void classifyBotChallengeAsRateLimit() {
        FailureCategory category = provider.classifyFailure(
                List.of("Sign in to confirm you're not a bot"),
                1
        );

        assertEquals(FailureCategory.RATE_LIMIT, category);
    }

    @Test
    void classifyInstagramCookieRequiredAsPrivateContent() {
        FailureCategory category = provider.classifyFailure(
                List.of("ERROR: [Instagram] DB-8TogOnu0: Instagram sent an empty media response. use --cookies"),
                1
        );

        assertEquals(FailureCategory.PRIVATE_CONTENT, category);
    }

    @Test
    void classifyTikTokIpBlockedAsPermissionDenied() {
        FailureCategory category = provider.classifyFailure(
                List.of("ERROR: [TikTok] 6758417933198085382: Your IP address is blocked from accessing this post"),
                1
        );

        assertEquals(FailureCategory.PERMISSION_DENIED, category);
    }

    @Test
    void classifyCannotParseDataAsUnsupported() {
        FailureCategory category = provider.classifyFailure(
                List.of("ERROR: [facebook] 10158223637116529: Cannot parse data"),
                1
        );

        assertEquals(FailureCategory.UNSUPPORTED, category);
    }

    @Test
    void buildDownloadCommandUsesNamedFfmpegPostProcessorArgs() {
        com.example.platform.downloader.domain.Job job = new com.example.platform.downloader.domain.Job();
        job.setUrl("https://www.youtube.com/watch?v=abc123");
        job.setDownloadType("VIDEO");
        job.setQuality("360");
        job.setFormat("mp4");
        job.setCleanMetadata(true);

        List<String> command = provider.buildDownloadCommand(job, Path.of("downloads/jobs/test"), null);

        assertTrue(command.contains("--postprocessor-args"));
        assertTrue(command.contains("FFmpeg:-map_metadata -1"));
    }
}
