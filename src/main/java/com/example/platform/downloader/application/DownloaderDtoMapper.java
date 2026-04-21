package com.example.platform.downloader.application;

import com.example.platform.downloader.domain.entity.Job;
import com.example.platform.downloader.domain.entity.SourceRequest;
import com.example.platform.downloader.ui.dto.JobFileResponse;
import com.example.platform.downloader.ui.dto.JobStatusResponse;
import com.example.platform.downloader.ui.dto.SourceRequestResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DownloaderDtoMapper {

    public JobStatusResponse toJobStatus(Job job, List<String> logs) {
        JobStatusResponse response = new JobStatusResponse();
        response.setId(job.getId());
        response.setSourceRequestId(job.getSourceRequest() != null ? job.getSourceRequest().getId() : null);
        response.setStatus(job.getStatus() != null ? job.getStatus().name() : null);
        response.setState(job.getState() != null ? job.getState().name() : null);
        response.setPlatform(job.getPlatform() != null ? job.getPlatform().name() : null);
        response.setSourceType(job.getSourceType() != null ? job.getSourceType().name() : null);
        response.setUrl(job.getUrl());
        response.setVideoTitle(job.getVideoTitle());
        response.setPlaylistTitle(job.getPlaylistTitle());
        response.setTotalItems(job.getTotalItems());
        response.setCurrentItem(job.getCurrentItem());
        response.setDownloadType(job.getDownloadType());
        response.setQuality(job.getQuality());
        response.setFormat(job.getFormat());
        response.setErrorMessage(job.getErrorMessage());
        response.setDownloadSpeed(job.getDownloadSpeed());
        response.setEta(job.getEta());
        response.setProgressPercent(job.getProgressPercent());
        response.setCreatedAt(job.getCreatedAt());
        response.setLogs(logs);
        return response;
    }

    public SourceRequestResponse toSourceRequest(SourceRequest sourceRequest, List<JobStatusResponse> jobs) {
        SourceRequestResponse response = new SourceRequestResponse();
        response.setId(sourceRequest.getId());
        response.setPlatform(sourceRequest.getPlatform() != null ? sourceRequest.getPlatform().name() : null);
        response.setSourceType(sourceRequest.getSourceType() != null ? sourceRequest.getSourceType().name() : null);
        response.setState(sourceRequest.getState() != null ? sourceRequest.getState().name() : null);
        response.setSourceUrl(sourceRequest.getSourceUrl());
        response.setResolvedCount(sourceRequest.getResolvedCount());
        response.setErrorMessage(sourceRequest.getErrorMessage());
        response.setBlockedReason(sourceRequest.getBlockedReason());
        response.setCreatedAt(sourceRequest.getCreatedAt());
        response.setJobs(jobs);
        return response;
    }

    public JobFileResponse toJobFile(String name, String path, String contentType, long size) {
        return toJobFile(name, path, null, contentType, null, size);
    }

    public JobFileResponse toJobFile(String name,
                                     String path,
                                     String downloadUrl,
                                     String contentType,
                                     String type,
                                     long size) {
        JobFileResponse response = new JobFileResponse();
        response.setName(name);
        response.setPath(path);
        response.setDownloadUrl(downloadUrl);
        response.setContentType(contentType);
        response.setType(type);
        response.setSize(size);
        return response;
    }
}
