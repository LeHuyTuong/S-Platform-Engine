package com.example.platform.downloader.application.provider;

import com.example.platform.downloader.domain.enums.FailureCategory;
import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.enums.Platform;
import com.example.platform.downloader.domain.entity.SourceRequest;

import java.nio.file.Path;
import java.util.List;

public interface ContentProvider {

    Platform platform();

    boolean supports(String url);

    SourceResolution resolveSource(SourceRequest request);

    List<String> buildDownloadCommand(Job job, Path jobDir, String cookieFilePath);

    ProgressSnapshot parseProgress(String line);

    FailureCategory classifyFailure(List<String> logs, int exitCode);
}
